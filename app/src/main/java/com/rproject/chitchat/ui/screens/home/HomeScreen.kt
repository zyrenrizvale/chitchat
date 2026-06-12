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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.rproject.chitchat.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val profilePicture: String = "",
    val phoneNumber: String = ""
)

data class ConversationPreview(
    val otherUser: UserProfile,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int = 0
)

// ─── Bottom Nav Items ──────────────────────────────────────────────────────────
enum class HomeTab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    Chats("Chats", Icons.Filled.Chat, Icons.Outlined.Chat),
    Contacts("Kontak", Icons.Filled.People, Icons.Outlined.PeopleOutline),
    Profile("Profil", Icons.Filled.Person, Icons.Outlined.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToChat: (String) -> Unit) {
    var selectedTab by remember { mutableStateOf(HomeTab.Chats) }
    var showNewChatSheet by remember { mutableStateOf(false) }
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    Scaffold(
        containerColor = ChitchatBgDark,
        bottomBar = {
            NavigationBar(
                containerColor = ChitchatSurfaceDark,
                tonalElevation = 0.dp,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = ChitchatOutline,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                HomeTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ChitchatPurple,
                            selectedTextColor = ChitchatPurple,
                            unselectedIconColor = ChitchatOnSurfaceVariant,
                            unselectedTextColor = ChitchatOnSurfaceVariant,
                            indicatorColor = ChitchatPurpleContainer
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == HomeTab.Chats) {
                FloatingActionButton(
                    onClick = { showNewChatSheet = true },
                    containerColor = ChitchatPurple,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "New Chat")
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab_content"
        ) { tab ->
            when (tab) {
                HomeTab.Chats -> ChatsTab(
                    modifier = Modifier.padding(padding),
                    currentUid = currentUid,
                    onNavigateToChat = onNavigateToChat
                )
                HomeTab.Contacts -> ContactsTab(
                    modifier = Modifier.padding(padding),
                    currentUid = currentUid,
                    onNavigateToChat = onNavigateToChat
                )
                HomeTab.Profile -> ProfileTab(
                    modifier = Modifier.padding(padding),
                    currentUid = currentUid
                )
            }
        }
    }

    // New Chat Bottom Sheet
    if (showNewChatSheet) {
        NewChatSheet(
            currentUid = currentUid,
            onDismiss = { showNewChatSheet = false },
            onNavigateToChat = { uid ->
                showNewChatSheet = false
                onNavigateToChat(uid)
            }
        )
    }
}

// ─── CHATS TAB ────────────────────────────────────────────────────────────────
@Composable
fun ChatsTab(modifier: Modifier, currentUid: String, onNavigateToChat: (String) -> Unit) {
    var conversations by remember { mutableStateOf<List<ConversationPreview>>(emptyList()) }
    var allUsers by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }

    // Load all users first
    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().getReference("users")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val map = mutableMapOf<String, UserProfile>()
                    for (child in snapshot.children) {
                        val u = child.getValue(UserProfile::class.java)?.copy(uid = child.key ?: "") ?: continue
                        map[u.uid] = u
                    }
                    allUsers = map
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // Load conversations
    LaunchedEffect(currentUid) {
        FirebaseDatabase.getInstance().getReference("chats")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<ConversationPreview>()
                    for (chatSnap in snapshot.children) {
                        val chatId = chatSnap.key ?: continue
                        if (!chatId.contains(currentUid)) continue
                        val otherUid = chatId.replace(currentUid, "").replace("_", "")
                        val otherUser = allUsers[otherUid] ?: continue
                        val messagesSnap = chatSnap.child("messages")
                        var lastMsg = ""
                        var lastTs = 0L
                        for (msgSnap in messagesSnap.children) {
                            val ts = msgSnap.child("timestamp").getValue(Long::class.java) ?: 0L
                            if (ts > lastTs) {
                                lastTs = ts
                                lastMsg = msgSnap.child("text").getValue(String::class.java) ?: ""
                            }
                        }
                        list.add(ConversationPreview(otherUser, lastMsg, lastTs))
                    }
                    conversations = list.sortedByDescending { it.lastTimestamp }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    Column(modifier = modifier.fillMaxSize()) {
        HomeTopBar(title = "Chitchat")
        if (conversations.isEmpty()) {
            EmptyState(
                emoji = "💬",
                title = "Belum ada percakapan",
                subtitle = "Tekan tombol ✏️ di bawah untuk memulai chat baru"
            )
        } else {
            LazyColumn {
                items(conversations) { conv ->
                    ConversationItem(conv = conv, onClick = { onNavigateToChat(conv.otherUser.uid) })
                    Divider(color = ChitchatOutline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 72.dp))
                }
            }
        }
    }
}

// ─── CONTACTS TAB ─────────────────────────────────────────────────────────────
@Composable
fun ContactsTab(modifier: Modifier, currentUid: String, onNavigateToChat: (String) -> Unit) {
    val context = LocalContext.current
    var allUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var contactNumbers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    // Load contact phone numbers
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val numbers = mutableSetOf<String>()
            val cursor = context.contentResolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null
            )
            cursor?.use {
                val colIdx = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val raw = it.getString(colIdx) ?: continue
                    val normalized = raw.replace("[^0-9+]".toRegex(), "")
                        .let { n -> if (n.startsWith("0")) "+62${n.substring(1)}" else n }
                    numbers.add(normalized)
                }
            }
            contactNumbers = numbers
        }
    }

    // Load Firebase users
    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().getReference("users")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<UserProfile>()
                    for (child in snapshot.children) {
                        val u = child.getValue(UserProfile::class.java)?.copy(uid = child.key ?: "") ?: continue
                        if (u.uid != currentUid) list.add(u)
                    }
                    allUsers = list
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    Column(modifier = modifier.fillMaxSize()) {
        HomeTopBar(title = "Kontak")

        if (!hasPermission) {
            // Permission request card
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📱", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Izin Kontak Diperlukan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ChitchatOnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Chitchat membutuhkan akses kontak untuk menampilkan teman yang sudah terdaftar.",
                        color = ChitchatOnSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                        colors = ButtonDefaults.buttonColors(containerColor = ChitchatPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Izinkan Akses Kontak", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            // Filter users whose phone is in contacts
            val chitchatContacts = allUsers.filter { user ->
                contactNumbers.any { num ->
                    val userNum = user.phoneNumber.replace("[^0-9+]".toRegex(), "")
                        .let { n -> if (n.startsWith("0")) "+62${n.substring(1)}" else n }
                    num == userNum || num.endsWith(userNum.takeLast(9))
                }
            }

            if (chitchatContacts.isEmpty()) {
                EmptyState(
                    emoji = "👥",
                    title = "Belum ada kontak di Chitchat",
                    subtitle = "Ajak teman kamu bergabung!"
                )
            } else {
                LazyColumn {
                    items(chitchatContacts) { user ->
                        UserListItem(user = user, onClick = { onNavigateToChat(user.uid) })
                        Divider(color = ChitchatOutline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 72.dp))
                    }
                }
            }
        }
    }
}

// ─── PROFILE TAB ──────────────────────────────────────────────────────────────
@Composable
fun ProfileTab(modifier: Modifier, currentUid: String) {
    var myProfile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(currentUid) {
        FirebaseDatabase.getInstance().getReference("users").child(currentUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    myProfile = snapshot.getValue(UserProfile::class.java)?.copy(uid = currentUid)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header bg
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(listOf(ChitchatPurple.copy(alpha = 0.3f), ChitchatBgDark))
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(40.dp))
                AvatarImage(
                    profilePicture = myProfile?.profilePicture ?: "",
                    name = myProfile?.name ?: "",
                    size = 88
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    myProfile?.name ?: "",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = ChitchatOnSurface
                )
                Text(
                    myProfile?.phoneNumber ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChitchatOnSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Menu items
        ProfileMenuItem(icon = Icons.Default.Notifications, label = "Notifikasi")
        ProfileMenuItem(icon = Icons.Default.Lock, label = "Privasi")
        ProfileMenuItem(icon = Icons.Default.Help, label = "Bantuan")

        Spacer(modifier = Modifier.height(12.dp))

        // Logout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ChitchatSurfaceDark)
                .clickable { FirebaseAuth.getInstance().signOut() }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = ChitchatError)
                Spacer(modifier = Modifier.width(14.dp))
                Text("Keluar", color = ChitchatError, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ChitchatSurfaceDark)
            .clickable { }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = ChitchatPurpleLight)
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, color = ChitchatOnSurface, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ChitchatOnSurfaceVariant)
        }
    }
}

// ─── NEW CHAT BOTTOM SHEET ────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatSheet(currentUid: String, onDismiss: () -> Unit, onNavigateToChat: (String) -> Unit) {
    var allUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var search by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().getReference("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<UserProfile>()
                    for (child in snapshot.children) {
                        val u = child.getValue(UserProfile::class.java)?.copy(uid = child.key ?: "") ?: continue
                        if (u.uid != currentUid) list.add(u)
                    }
                    allUsers = list
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    val filtered = allUsers.filter { it.name.contains(search, ignoreCase = true) || it.phoneNumber.contains(search) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ChitchatSurfaceDark,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text(
                "Chat Baru",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = ChitchatOnSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Search
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ChitchatSurface2Dark)
                    .border(1.dp, ChitchatOutline, RoundedCornerShape(14.dp))
            ) {
                TextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari nama atau nomor...", color = ChitchatOnSurfaceVariant.copy(0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ChitchatOnSurfaceVariant) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = ChitchatOnSurface,
                        unfocusedTextColor = ChitchatOnSurface,
                        cursorColor = ChitchatPurple
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Tidak ada pengguna ditemukan", color = ChitchatOnSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(filtered) { user ->
                        UserListItem(user = user, onClick = { onNavigateToChat(user.uid) })
                    }
                }
            }
        }
    }
}

// ─── SHARED COMPONENTS ────────────────────────────────────────────────────────
@Composable
fun HomeTopBar(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChitchatBgDark)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                brush = Brush.horizontalGradient(listOf(ChitchatPurpleLight, ChitchatPurple))
            )
        )
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(emoji, fontSize = 56.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ChitchatOnSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, color = ChitchatOnSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
fun ConversationItem(conv: ConversationPreview, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(profilePicture = conv.otherUser.profilePicture, name = conv.otherUser.name, size = 52)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                conv.otherUser.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = ChitchatOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                conv.lastMessage,
                style = MaterialTheme.typography.bodySmall,
                color = ChitchatOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (conv.lastTimestamp > 0L) {
            Text(
                formatTimestamp(conv.lastTimestamp),
                style = MaterialTheme.typography.labelSmall,
                color = ChitchatOnSurfaceVariant
            )
        }
    }
}

@Composable
fun UserListItem(user: UserProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(profilePicture = user.profilePicture, name = user.name, size = 52)
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                user.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = ChitchatOnSurface
            )
            if (user.phoneNumber.isNotEmpty()) {
                Text(user.phoneNumber, style = MaterialTheme.typography.bodySmall, color = ChitchatOnSurfaceVariant)
            }
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
        // Initials avatar
        val initials = name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(ChitchatPurple, ChitchatPurpleDark))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials.ifEmpty { "?" },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size / 2.8).sp
            )
        }
    }
}

fun formatTimestamp(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    return when {
        diff < 60_000 -> "baru saja"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(ts))
    }
}
