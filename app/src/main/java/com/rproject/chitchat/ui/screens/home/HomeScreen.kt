package com.rproject.chitchat.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.rproject.chitchat.ui.theme.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

// ─── Data Models ──────────────────────────────────────────────────────────────
data class UserProfile(val uid: String = "", val name: String = "", val phoneNumber: String = "", val profilePicture: String = "")
data class Conversation(val userId: String, val name: String, val lastMessage: String, val timestamp: Long, val profilePicture: String)
data class Story(val userId: String, val name: String, val profilePicture: String, val isMine: Boolean = false)

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
    var stories by remember { mutableStateOf<List<Story>>(emptyList()) }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    // Fetch user profile, chats, stories
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        val db = FirebaseDatabase.getInstance()

        db.getReference("users").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { myProfile = s.getValue(UserProfile::class.java)?.copy(uid = s.key ?: "") }
            override fun onCancelled(e: DatabaseError) {}
        })

        // Fetch contacts globally to map names for conversations
        val usersMap = mutableMapOf<String, UserProfile>()
        db.getReference("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = mutableListOf<UserProfile>()
                val stList = mutableListOf<Story>()
                stList.add(Story(uid, "My Story", "", true)) // Placeholder for own story
                
                for (userSnap in s.children) {
                    val u = userSnap.getValue(UserProfile::class.java)?.copy(uid = userSnap.key ?: "")
                    if (u != null) {
                        usersMap[u.uid] = u
                        if (u.uid != uid) {
                            list.add(u)
                            // Simulate 20% of users having a story
                            if (u.name.length % 3 == 0) {
                                stList.add(Story(u.uid, u.name, u.profilePicture))
                            }
                        }
                    }
                }
                chitchatContacts = list
                stories = stList
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        // Fetch Chats
        db.getReference("chats").child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = mutableListOf<Conversation>()
                for (chatSnap in s.children) {
                    val otherUserId = chatSnap.key ?: continue
                    val lastMsg = chatSnap.child("lastMessage").getValue(String::class.java) ?: "..."
                    val ts = chatSnap.child("timestamp").getValue(Long::class.java) ?: 0L
                    
                    // Cross-reference name from usersMap
                    val contactUser = usersMap[otherUserId]
                    val dispName = contactUser?.name ?: "Unknown"
                    val dispPic = contactUser?.profilePicture ?: ""
                    
                    list.add(Conversation(otherUserId, dispName, lastMsg, ts, dispPic))
                }
                conversations = list.sortedByDescending { it.timestamp }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            BottomNavBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                val fabInteractionSource = remember { MutableInteractionSource() }
                val isPressed by fabInteractionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1f, label = "fabScale")

                FloatingActionButton(
                    onClick = { showNewChatSheet = true },
                    containerColor = BrandPurple,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.scale(scale),
                    interactionSource = fabInteractionSource
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = selectedTab, 
                label = "tabs",
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                }
            ) { tab ->
                when (tab) {
                    0 -> ChatsTab(stories, conversations, onNavigateToChat)
                    1 -> ContactsTab(hasPermission, chitchatContacts, onNavigateToChat) { launcher.launch(Manifest.permission.READ_CONTACTS) }
                    2 -> ProfileTab(myProfile)
                }
            }
        }

        if (showNewChatSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNewChatSheet = false },
                containerColor = SurfaceWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Select Contact", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!hasPermission) {
                        Text("Contact permission required", color = TextSecondary)
                    } else if (chitchatContacts.isEmpty()) {
                        Text("No contacts found on Chitchat", color = TextSecondary)
                    } else {
                        LazyColumn {
                            itemsIndexed(chitchatContacts) { index, contact ->
                                AnimatedListItem(index = index) {
                                    ContactItem(contact = contact) {
                                        showNewChatSheet = false
                                        onNavigateToChat(contact.uid)
                                    }
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
        tonalElevation = 16.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(if (selectedTab == 0) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline, null) },
            label = { Text("Chats", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BrandPurple, unselectedIconColor = TextSecondary,
                selectedTextColor = BrandPurple, unselectedTextColor = TextSecondary,
                indicatorColor = BrandPurpleLight
            )
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(if (selectedTab == 1) Icons.Filled.People else Icons.Outlined.PeopleOutline, null) },
            label = { Text("Contacts", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BrandPurple, unselectedIconColor = TextSecondary,
                selectedTextColor = BrandPurple, unselectedTextColor = TextSecondary,
                indicatorColor = BrandPurpleLight
            )
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(if (selectedTab == 2) Icons.Filled.Person else Icons.Outlined.PersonOutline, null) },
            label = { Text("Profile", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BrandPurple, unselectedIconColor = TextSecondary,
                selectedTextColor = BrandPurple, unselectedTextColor = TextSecondary,
                indicatorColor = BrandPurpleLight
            )
        )
    }
}

// ─── CHATS TAB (WITH STORIES) ────────────────────────────────────────────────
@Composable
fun ChatsTab(stories: List<Story>, conversations: List<Conversation>, onNavigateToChat: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        HomeTopBar(title = "Chats")
        
        // Stories Row
        if (stories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(stories) { idx, story ->
                    StoryItem(story)
                }
            }
            Divider(color = SurfaceGray, thickness = 1.dp)
        }

        if (conversations.isEmpty()) {
            EmptyState(icon = Icons.Filled.Forum, title = "No conversations yet", subtitle = "Tap the + button to start a new chat")
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)) {
                itemsIndexed(conversations) { index, chat ->
                    AnimatedListItem(index = index) {
                        ChatItem(chat = chat) { onNavigateToChat(chat.userId) }
                    }
                }
            }
        }
    }
}

@Composable
fun StoryItem(story: Story) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(2.dp, if (story.isMine) BrandBlue else BrandPurple, CircleShape)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (story.isMine && story.profilePicture.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(BrandBlue), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                }
            } else {
                AvatarImage(profilePicture = story.profilePicture, name = story.name, size = 56)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(if (story.isMine) "You" else story.name, fontSize = 12.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ChatItem(chat: Conversation, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, label = "itemScale")
    val bgAlpha by animateFloatAsState(targetValue = if (isPressed) 0.05f else 0f, label = "itemBg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(Color.Black.copy(alpha = bgAlpha))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
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
            Spacer(modifier = Modifier.height(2.dp))
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
                    itemsIndexed(contacts) { index, contact ->
                        AnimatedListItem(index = index) {
                            ContactItem(contact = contact) { onNavigateToChat(contact.uid) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(contact: UserProfile, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, label = "itemScale")
    val bgAlpha by animateFloatAsState(targetValue = if (isPressed) 0.05f else 0f, label = "itemBg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(Color.Black.copy(alpha = bgAlpha))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
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

// ─── PROFILE TAB (EDITABLE) ──────────────────────────────────────────────────
@Composable
fun ProfileTab(myProfile: UserProfile?) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(myProfile?.name ?: "") }
    var editImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(myProfile) { if (!isEditing) editName = myProfile?.name ?: "" }
    LaunchedEffect(Unit) { visible = true }
    
    val slideOffset by animateFloatAsState(targetValue = if (visible) 0f else 40f, animationSpec = tween(600, easing = EaseOutExpo))
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(600, easing = EaseOutExpo))

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> editImageUri = uri }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HomeTopBar(title = "Profile")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 24.dp)
                .offset(y = slideOffset.dp)
                .alpha(alpha),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    if (editImageUri != null) {
                        AsyncImage(model = editImageUri, contentDescription = null, modifier = Modifier.size(100.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        AvatarImage(profilePicture = myProfile?.profilePicture ?: "", name = myProfile?.name ?: "", size = 100)
                    }
                    
                    if (isEditing) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(BrandBlue).border(2.dp, Color.White, CircleShape).clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isEditing) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        singleLine = true,
                        modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@Button
                            isSaving = true
                            
                            var newBase64 = myProfile?.profilePicture ?: ""
                            if (editImageUri != null) {
                                try {
                                    val inputStream = context.contentResolver.openInputStream(editImageUri!!)
                                    val bitmap = BitmapFactory.decodeStream(inputStream)
                                    val resized = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
                                    val baos = ByteArrayOutputStream()
                                    resized.compress(Bitmap.CompressFormat.JPEG, 60, baos)
                                    newBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                                } catch (e: Exception) {}
                            }

                            FirebaseDatabase.getInstance().getReference("users").child(uid)
                                .updateChildren(mapOf("name" to editName, "profilePicture" to newBase64))
                                .addOnCompleteListener { 
                                    isSaving = false
                                    isEditing = false 
                                    editImageUri = null
                                }
                        },
                        enabled = editName.isNotBlank() && !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                    ) { Text(if (isSaving) "Saving..." else "Save Changes") }
                } else {
                    Text(myProfile?.name ?: "", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(myProfile?.phoneNumber ?: "", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                    TextButton(onClick = { isEditing = true }) { Text("Edit Profile", color = BrandBlue) }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.offset(y = (slideOffset * 0.8f).dp).alpha(alpha)) {
            // Dark mode switch is omitted here, we will use a global ThemeManager wrapper later
            ProfileMenuItem(icon = Icons.Outlined.DarkMode, label = "Dark Mode")
            ProfileMenuItem(icon = Icons.Outlined.Notifications, label = "Notifications")
            ProfileMenuItem(icon = Icons.Outlined.Lock, label = "Privacy")
            ProfileMenuItem(icon = Icons.Outlined.HelpOutline, label = "Help")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .offset(y = (slideOffset * 0.6f).dp)
                .alpha(alpha)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceWhite)
                .clickable { FirebaseAuth.getInstance().signOut() }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Logout, null, tint = ErrorColor)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Logout", color = ErrorColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, label: String) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgAlpha by animateFloatAsState(targetValue = if (isPressed) 0.05f else 0f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = bgAlpha))
            .clickable(interactionSource = interactionSource, indication = null) { }
            .padding(horizontal = 24.dp, vertical = 14.dp),
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
fun AnimatedListItem(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 30L)
        visible = true
    }
    
    val slideOffset by animateFloatAsState(targetValue = if (visible) 0f else 20f, animationSpec = tween(400, easing = EaseOutExpo), label = "itemSlide")
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(400, easing = EaseOutExpo), label = "itemAlpha")

    Box(modifier = Modifier.offset(y = slideOffset.dp).alpha(alpha)) { content() }
}

@Composable
fun HomeTopBar(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(SurfaceGray).clickable { },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Search, null, tint = TextPrimary, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(targetValue = if (visible) 1f else 0.8f, animationSpec = tween(600, easing = EaseOutBack))
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(600))

    Box(modifier = Modifier.fillMaxSize().scale(scale).alpha(alpha), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(SurfaceGray),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = BrandBlue, modifier = Modifier.size(36.dp)) }
            Spacer(modifier = Modifier.height(20.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, color = TextSecondary, textAlign = TextAlign.Center, fontSize = 14.sp)
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
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(size.dp).clip(CircleShape), contentScale = ContentScale.Crop)
    } else {
        val initials = name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
        if (initials.isNotEmpty()) {
            Box(modifier = Modifier.size(size.dp).clip(CircleShape).background(BrandBlue), contentAlignment = Alignment.Center) {
                Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size / 2.8).sp)
            }
        } else {
            Box(modifier = Modifier.size(size.dp).clip(CircleShape).background(SurfaceGray), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Person, null, tint = TextHint, modifier = Modifier.size((size * 0.55f).dp))
            }
        }
    }
}
