package com.rproject.chitchat.ui.screens.home

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val profilePicture: String = "",
    val phoneNumber: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToChat: (String) -> Unit) {
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(Unit) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<UserProfile>()
                for (child in snapshot.children) {
                    val user = child.getValue(UserProfile::class.java)?.copy(uid = child.key ?: "")
                    if (user != null && user.uid != currentUid) {
                        list.add(user)
                    }
                }
                users = list
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Chitchat") })
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            items(users) { user ->
                UserListItem(user = user, onClick = { onNavigateToChat(user.uid) })
            }
        }
    }
}

@Composable
fun UserListItem(user: UserProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (user.profilePicture.isNotEmpty()) {
            try {
                val bytes = Base64.decode(user.profilePicture, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } catch (e: Exception) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape))
            }
        } else {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {}
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(user.name, style = MaterialTheme.typography.titleMedium)
            Text(user.phoneNumber, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
