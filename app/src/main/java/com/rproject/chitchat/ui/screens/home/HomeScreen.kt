package com.rproject.chitchat.ui.screens.home

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.rproject.chitchat.ui.theme.*
import java.util.UUID

@Composable
fun HomeScreen(onNavigateToChat: (String) -> Unit, onNavigateToCreateGroup: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        containerColor = AppBackground,
        bottomBar = { HomeBottomNav(selectedTab) { selectedTab = it } },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onNavigateToCreateGroup,
                    containerColor = BrandBlue,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "New Group")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> ChatsTab(onNavigateToChat)
                1 -> CallsTab()
                2 -> ProfileTab()
            }
        }
    }
}

@Composable
fun ChatsTab(onNavigateToChat: (String) -> Unit) {
    val db = FirebaseDatabase.getInstance()
    val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    
    var chats by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var stories by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var isUploadingStory by remember { mutableStateOf(false) }
    var viewedStory by remember { mutableStateOf<String?>(null) }
    
    val storage = FirebaseStorage.getInstance()
    
    val storyPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            isUploadingStory = true
            val ref = storage.reference.child("stories/${UUID.randomUUID()}.jpg")
            ref.putFile(uri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    val storyData = mapOf("imageUrl" to downloadUri.toString(), "timestamp" to System.currentTimeMillis())
                    db.getReference("stories").child(myUid).setValue(storyData)
                    isUploadingStory = false
                }
            }.addOnFailureListener { isUploadingStory = false }
        }
    }

    LaunchedEffect(Unit) {
        db.getReference("stories").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = mutableListOf<Map<String, String>>()
                for (u in s.children) {
                    val uid = u.key ?: continue
                    val imageUrl = u.child("imageUrl").getValue(String::class.java) ?: ""
                    db.getReference("users").child(uid).get().addOnSuccessListener { us ->
                        val name = us.child("name").getValue(String::class.java) ?: "User"
                        val pp = us.child("profilePicture").getValue(String::class.java) ?: ""
                        list.add(mapOf("uid" to uid, "name" to name, "profilePicture" to pp, "imageUrl" to imageUrl))
                        stories = list.toList()
                    }
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        // Fetch Chats (Users + Groups)
        db.getReference("chats").child(myUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = mutableListOf<Map<String, Any>>()
                for (c in s.children) {
                    val id = c.key ?: continue
                    val lastMsg = c.child("lastMessage").getValue(String::class.java) ?: ""
                    val ts = c.child("timestamp").getValue(Long::class.java) ?: 0L
                    
                    if (id.startsWith("group_")) {
                        db.getReference("groups").child(id).get().addOnSuccessListener { gs ->
                            val name = gs.child("name").getValue(String::class.java) ?: "Group"
                            val icon = gs.child("icon").getValue(String::class.java) ?: ""
                            list.add(mapOf("id" to id, "name" to name, "icon" to icon, "lastMsg" to lastMsg, "timestamp" to ts, "isGroup" to true))
                            chats = list.sortedByDescending { it["timestamp"] as Long }
                        }
                    } else {
                        db.getReference("users").child(id).get().addOnSuccessListener { us ->
                            val name = us.child("name").getValue(String::class.java) ?: "User"
                            val pp = us.child("profilePicture").getValue(String::class.java) ?: ""
                            list.add(mapOf("id" to id, "name" to name, "icon" to pp, "lastMsg" to lastMsg, "timestamp" to ts, "isGroup" to false))
                            chats = list.sortedByDescending { it["timestamp"] as Long }
                        }
                    }
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HomeHeader()
        
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { storyPicker.launch("image/*") }) {
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(BrandBlue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                        if (isUploadingStory) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BrandBlue, strokeWidth = 2.dp)
                        else Icon(Icons.Default.Add, null, tint = BrandBlue)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("My Story", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = TextPrimary)
                }
            }
            items(stories) { story ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { viewedStory = story["imageUrl"] }) {
                    val bitmap = remember(story["profilePicture"]) {
                        try {
                            val bytes = Base64.decode(story["profilePicture"], Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
                        } catch (e: Exception) { null }
                    }
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).border(2.dp, BrandBlue, CircleShape).padding(4.dp)) {
                        if (bitmap != null) Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        else Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(BrandPurple), contentAlignment = Alignment.Center) {
                            Text(story["name"]!!.take(1).uppercase(), color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(story["name"]!!, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(64.dp))
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = SurfaceWhite,
            shadowElevation = 8.dp
        ) {
            LazyColumn(contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)) {
                items(chats) { chat ->
                    ChatItem(
                        name = chat["name"] as String,
                        lastMessage = chat["lastMsg"] as String,
                        time = "10:00 AM", // TBD format timestamp
                        unreadCount = 0,
                        profilePicture = chat["icon"] as String,
                        isGroup = chat["isGroup"] as Boolean,
                        onClick = { onNavigateToChat(chat["id"] as String) }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Chitchat", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        IconButton(onClick = {}) { Icon(Icons.Outlined.Search, contentDescription = "Search", tint = TextPrimary) }
    }
}

@Composable
fun ChatItem(name: String, lastMessage: String, time: String, unreadCount: Int, profilePicture: String, isGroup: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmap = remember(profilePicture) {
            if (profilePicture.isNotEmpty()) {
                try {
                    val bytes = Base64.decode(profilePicture, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
                } catch (e: Exception) { null }
            } else null
        }

        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(56.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(if (isGroup) BrandPurple else BrandBlue), contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(lastMessage, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(time, style = MaterialTheme.typography.labelSmall, color = if (unreadCount > 0) BrandBlue else TextHint, fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal)
            Spacer(modifier = Modifier.height(6.dp))
            if (unreadCount > 0) {
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(BrandBlue), contentAlignment = Alignment.Center) {
                    Text(unreadCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CallsTab() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Calls History (Coming Soon)", color = TextHint)
    }
}

@Composable
fun ProfileTab() {
    val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var name by remember { mutableStateOf("Loading...") }
    var isDarkMode by remember { mutableStateOf(false) }
    var isNotifications by remember { mutableStateOf(true) }
    var isPrivacy by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().getReference("users").child(myUid).get().addOnSuccessListener { s ->
            name = s.child("name").getValue(String::class.java) ?: "User"
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().background(SurfaceWhite).padding(24.dp)) {
        Spacer(modifier = Modifier.height(40.dp))
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(BrandBlue), contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Available", style = MaterialTheme.typography.bodyMedium, color = BrandBlue)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Divider(color = SurfaceGray)
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingToggleRow("Dark Mode", Icons.Outlined.DarkMode, isDarkMode) { isDarkMode = it }
        SettingToggleRow("Notifications", Icons.Outlined.Notifications, isNotifications) { isNotifications = it }
        SettingToggleRow("Privacy Lock", Icons.Outlined.Lock, isPrivacy) { isPrivacy = it }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { FirebaseAuth.getInstance().signOut() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ErrorColor.copy(alpha=0.1f), contentColor = ErrorColor),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Text("Logout", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingToggleRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceGray), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandBlue))
    }
}

@Composable
fun HomeBottomNav(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = SurfaceWhite,
        contentColor = TextPrimary,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(if (selectedTab == 0) Icons.Default.Chat else Icons.Outlined.Chat, contentDescription = "Chats") },
            label = { Text("Chats") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = BrandBlue, selectedTextColor = BrandBlue, unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary, indicatorColor = BrandBlue.copy(alpha=0.1f))
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(if (selectedTab == 1) Icons.Default.Call else Icons.Outlined.Call, contentDescription = "Calls") },
            label = { Text("Calls") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = BrandBlue, selectedTextColor = BrandBlue, unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary, indicatorColor = BrandBlue.copy(alpha=0.1f))
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(if (selectedTab == 2) Icons.Default.Person else Icons.Outlined.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = BrandBlue, selectedTextColor = BrandBlue, unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary, indicatorColor = BrandBlue.copy(alpha=0.1f))
        )
    }
}
