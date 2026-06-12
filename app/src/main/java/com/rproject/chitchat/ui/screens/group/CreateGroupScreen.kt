package com.rproject.chitchat.ui.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.rproject.chitchat.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(onBack: () -> Unit, onGroupCreated: (String) -> Unit) {
    var groupName by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    val selectedUsers = remember { mutableStateListOf<String>() }
    var isLoading by remember { mutableStateOf(true) }

    val db = FirebaseDatabase.getInstance()
    val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    LaunchedEffect(Unit) {
        db.getReference("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = mutableListOf<Map<String, String>>()
                for (u in s.children) {
                    if (u.key != myUid) {
                        list.add(mapOf(
                            "id" to (u.key ?: ""),
                            "name" to (u.child("name").getValue(String::class.java) ?: "User")
                        ))
                    }
                }
                users = list
                isLoading = false
            }
            override fun onCancelled(e: DatabaseError) { isLoading = false }
        })
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(SurfaceWhite).padding(top = 40.dp, bottom = 12.dp, start = 8.dp, end = 16.dp).shadow(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
                Text("New Group", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        floatingActionButton = {
            if (groupName.isNotBlank() && selectedUsers.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        val groupId = "group_${UUID.randomUUID().toString().take(8)}"
                        val members = selectedUsers.toList() + myUid
                        val groupData = mapOf(
                            "id" to groupId,
                            "name" to groupName,
                            "admin" to myUid,
                            "members" to members
                        )
                        db.getReference("groups").child(groupId).setValue(groupData).addOnSuccessListener {
                            // Add group to each member's chat list
                            members.forEach { memberUid ->
                                val update = mapOf(
                                    "chats/$memberUid/$groupId/lastMessage" to "Group created",
                                    "chats/$memberUid/$groupId/timestamp" to System.currentTimeMillis()
                                )
                                db.reference.updateChildren(update)
                            }
                            onGroupCreated(groupId)
                        }
                    },
                    containerColor = BrandBlue,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Create Group")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.GroupAdd, null, tint = BrandBlue) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Select Members (${selectedUsers.size})", fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(users) { user ->
                        val isSelected = selectedUsers.contains(user["id"])
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) BrandBlue.copy(alpha = 0.1f) else SurfaceWhite)
                                .clickable {
                                    if (isSelected) selectedUsers.remove(user["id"])
                                    else selectedUsers.add(user["id"]!!)
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(BrandBlue), contentAlignment = Alignment.Center) {
                                Text(user["name"]!!.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(user["name"]!!, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = BrandBlue)
                            }
                        }
                    }
                }
            }
        }
    }
}
