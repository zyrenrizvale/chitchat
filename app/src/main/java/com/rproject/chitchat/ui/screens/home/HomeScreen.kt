package com.rproject.chitchat.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.rproject.chitchat.ui.theme.*

// ─── Data Models ──────────────────────────────────────────────────────────────
data class UserProfile(val uid: String = "", val name: String = "", val phoneNumber: String = "", val profilePicture: String = "")
data class Conversation(val userId: String, val name: String, val lastMessage: String, val timestamp: Long, val profilePicture: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToChat: (String) -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var showNewChatSheet by remember { mutableStateOf(false) }

    // State
    var myProfile by remember { mutableStateOf<UserProfile?>(null) }
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var chitchatContacts by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    // Fetch user profile & chats
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        val db = FirebaseDatabase.getInstance()

        // My Profile
        db.getReference("users").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { myProfile = s.getValue(UserProfile::class.java)?.copy(uid = s.key ?: "") }
            override fun onCancelled(e: DatabaseError) {}
        })

        // Conversations Mock/Dummy logic (replace with real listener later)
        db.getReference("chats").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = mutableListOf<Conversation>()
                for (chatSnap in s.children) {
                    val otherUserId = chatSnap.key ?: continue
                    val lastMsg = chatSnap.child("lastMessage").getValue(String::class.java) ?: "..."
                    val ts = chatSnap.child("timestamp").getValue(Long::class.java) ?: 0L
                    list.add(Conversation(otherUserId, "User", lastMsg, ts, ""))
                }
                conversations = list.sortedByDescending { it.timestamp }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    // Contact matching
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            // Simulated contact sync for now
            val db = FirebaseDatabase.getInstance()
            db.getReference("users").get().addOnSuccessListener { s ->
                val list = mutableListOf<UserProfile>()
                val myUid = FirebaseAuth.getInstance().currentUser?.uid
                for (userSnap in s.children) {
                    val u = userSnap.getValue(UserProfile::class.java)?.copy(uid = userSnap.key ?: "")
                    if (u != null && u.uid != myUid) list.add(u)
                }
                chitchatContacts = list
            }
        }
    }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showNewChatSheet = true },
                    containerColor = BrandPurple,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(targetState = selectedTab, label = "tabs") { tab ->
                when (tab) {
                    0 -> ChatsTab(conversations, onNavigateToChat)
                    1 -> ContactsTab(hasPermission, chitchatContacts, onNavigateToChat) { launcher.launch(Manifest.permission.READ_CONTACTS) }
                    2 -> ProfileTab(myProfile)
                }
            }
        }

        if (showNewChatSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNewChatSheet = false },
                containerColor = SurfaceWhite
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Contact", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!hasPermission) {
                        Text("Contact permission required", color = TextSecondary)
                    } else if (chitchatContacts.isEmpty()) {
                        Text("No contacts found on Chitchat", color = TextSecondary)
                    } else {
                        LazyColumn {
                            items(chitchatContacts) { contact ->
                                ContactItem(contact = contact) {
                                    showNewChatSheet = false
                                    onNavigateToChat(contact.uid)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ─── BOTTOM NAV ───────────────────────────────────────────────────────────────
@Composable
fun BottomNavBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = SurfaceWhite,
        contentColor = BrandPurple,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(if (selectedTab == 0) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline, null) },
            label = { Text("Chats") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BrandPurple,
                unselectedIconColor = TextSecondary,
                selectedTextColor = BrandPurple,
                unselectedTextColor = TextSecondary,
                indicatorColor = BrandPurpleLight
            )
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(if (selectedTab == 1) Icons.Filled.People else Icons.Outlined.PeopleOutline, null) },
            label = { Text("Contacts") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BrandPurple,
                unselectedIconColor = TextSecondary,
                selectedTextColor = BrandPurple,
                unselectedTextColor = TextSecondary,
                indicatorColor = BrandPurpleLight
            )
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(if (selectedTab == 2) Icons.Filled.Person else Icons.Outlined.PersonOutline, null) },
            label = { Text("Profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BrandPurple,
                unselectedIconColor = TextSecondary,
                selectedTextColor = BrandPurple,
                unselectedTextColor = TextSecondary,
                indicatorColor = BrandPurpleLight
            )
        )
    }
}

// ─── CHATS TAB ────────────────────────────────────────────────────────────────
@Composable
fun ChatsTab(conversations: List<Conversation>, onNavigateToChat: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        HomeTopBar(title = "Chat List")
        if (conversations.isEmpty()) {
            EmptyState(icon = Icons.Filled.Forum, title = "No conversations yet", subtitle = "Tap the + button to start a new chat")
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)) {
                items(conversations) { chat ->
                    ChatItem(chat = chat) { onNavigateToChat(chat.userId) }
                }
            }
        }
    }
}

@Composable
fun ChatItem(chat: Conversation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(profilePicture = chat.profilePicture, name = chat.name, size = 52)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(chat.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("10:00 AM", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(chat.lastMessage, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─── CONTACTS TAB ─────────────────────────────────────────────────────────────
@Composable
fun ContactsTab(hasPermission: Boolean, contacts: List<UserProfile>, onNavigateToChat: (String) -> Unit, onRequestPermission: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        HomeTopBar(title = "Contacts")

        if (!hasPermission) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Contacts, null, tint = BrandBlue, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Contacts Permission Required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Chitchat needs access to your contacts to find friends.", textAlign = TextAlign.Center, color = TextSecondary)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onRequestPermission, colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)) {
                        Text("Allow Access")
                    }
                }
            }
        } else {
            if (contacts.isEmpty()) {
                EmptyState(icon = Icons.Filled.PersonSearch, title = "No contacts found", subtitle = "Invite your friends to Chitchat!")
            } else {
                LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)) {
                    items(contacts) { contact ->
                        ContactItem(contact = contact) { onNavigateToChat(contact.uid) }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(contact: UserProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(profilePicture = contact.profilePicture, name = contact.name, size = 52)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(contact.phoneNumber, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

// ─── PROFILE TAB ──────────────────────────────────────────────────────────────
@Composable
fun ProfileTab(myProfile: UserProfile?) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HomeTopBar(title = "Profile")

        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AvatarImage(profilePicture = myProfile?.profilePicture ?: "", name = myProfile?.name ?: "", size = 100)
                Spacer(modifier = Modifier.height(16.dp))
                Text(myProfile?.name ?: "", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(myProfile?.phoneNumber ?: "", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProfileMenuItem(icon = Icons.Outlined.Notifications, label = "Notifications")
        ProfileMenuItem(icon = Icons.Outlined.Lock, label = "Privacy")
        ProfileMenuItem(icon = Icons.Outlined.HelpOutline, label = "Help")

        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceWhite)
                .clickable { FirebaseAuth.getInstance().signOut() }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Logout, null, tint = ErrorColor)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Logout", color = ErrorColor, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(BrandPurpleLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = BrandPurple, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = TextHint)
    }
}

// ─── SHARED COMPONENTS ────────────────────────────────────────────────────────
@Composable
fun HomeTopBar(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        IconButton(onClick = { /* Search */ }) {
            Icon(Icons.Outlined.Search, null, tint = TextPrimary)
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(SurfaceGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = BrandBlue, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AvatarImage(profilePicture: String, name: String, size: Int) {
    val bitmap = remember(profilePicture) {
        if (profilePicture.isNotEmpty()) {
            try {
                val bytes = Base64.decode(profilePicture, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) { null }
        } else null
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(size.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val initials = name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
        if (initials.isNotEmpty()) {
            Box(
                modifier = Modifier.size(size.dp).clip(CircleShape).background(BrandBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size / 2.8).sp)
            }
        } else {
            Box(
                modifier = Modifier.size(size.dp).clip(CircleShape).background(SurfaceGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, null, tint = TextHint, modifier = Modifier.size((size * 0.55f).dp))
            }
        }
    }
}
