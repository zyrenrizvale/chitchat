package com.rproject.chitchat.ui.screens.chat

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.rproject.chitchat.ui.theme.*
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import java.util.UUID

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val audioUrl: String = "",
    val timestamp: Long = 0L,
    val status: String = "SENT" // SENT, DELIVERED, READ
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(otherUserId: String, onBack: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var otherUserName by remember { mutableStateOf("User") }
    var otherUserImage by remember { mutableStateOf("") }
    var isGroup by remember { mutableStateOf(otherUserId.startsWith("group_")) }
    
    val context = LocalContext.current
    val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseDatabase.getInstance()
    val storage = FirebaseStorage.getInstance()
    
    val chatId = if (isGroup) otherUserId else if (myUid < otherUserId) "${myUid}_$otherUserId" else "${otherUserId}_$myUid"
    val listState = rememberLazyListState()

    var showActionMenu by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            val ref = storage.reference.child("chat_images/${UUID.randomUUID()}.jpg")
            ref.putFile(uri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    val msg = ChatMessage(senderId = myUid, imageUrl = downloadUri.toString(), timestamp = System.currentTimeMillis(), status = "SENT")
                    db.getReference("messages").child(chatId).push().setValue(msg)
                    updateRecentChat(db, myUid, otherUserId, isGroup, "📷 Photo", msg.timestamp)
                    isUploading = false
                }
            }.addOnFailureListener { isUploading = false }
        }
    }

    // Initialize ZegoCloud once
    LaunchedEffect(Unit) {
        val appID: Long = 123456789L // TBD: REPLACE WITH REAL ZEGOCLOUD APP ID
        val appSign = "your_zego_app_sign_here" // TBD: REPLACE WITH REAL SIGN
        
        db.getReference("users").child(myUid).get().addOnSuccessListener { s ->
            val myName = s.child("name").getValue(String::class.java) ?: "User"
            try {
                ZegoUIKitPrebuiltCallService.init(
                    context.applicationContext as Application,
                    appID,
                    appSign,
                    myUid,
                    myName,
                    ZegoUIKitPrebuiltCallInvitationConfig()
                )
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // Fetch Target Info
    LaunchedEffect(otherUserId) {
        if (isGroup) {
            db.getReference("groups").child(otherUserId).get().addOnSuccessListener { s ->
                otherUserName = s.child("name").getValue(String::class.java) ?: "Group"
                otherUserImage = s.child("icon").getValue(String::class.java) ?: ""
            }
        } else {
            db.getReference("users").child(otherUserId).get().addOnSuccessListener { s ->
                otherUserName = s.child("name").getValue(String::class.java) ?: "User"
                otherUserImage = s.child("profilePicture").getValue(String::class.java) ?: ""
            }
        }
    }

    // Fetch Messages & Handle Read Receipts
    LaunchedEffect(chatId) {
        db.getReference("messages").child(chatId).orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    val list = mutableListOf<ChatMessage>()
                    var needsUpdate = false
                    for (m in s.children) {
                        val msg = m.getValue(ChatMessage::class.java)?.copy(id = m.key ?: "")
                        if (msg != null) {
                            list.add(msg)
                            if (msg.senderId != myUid && msg.status != "READ") {
                                m.ref.child("status").setValue("READ")
                                needsUpdate = true
                            }
                        }
                    }
                    messages = list
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            ChatTopBar(
                name = otherUserName,
                profilePicture = otherUserImage,
                otherUserId = otherUserId,
                isGroup = isGroup,
                onBack = onBack,
                onSettingsClick = { /* Show Settings Dialog later */ }
            )
        },
        bottomBar = {
            ChatInputBar(
                value = messageText,
                onValueChange = { messageText = it },
                onAttachClick = { imagePicker.launch("image/*") },
                onSend = {
                    if (messageText.isNotBlank()) {
                        val ref = db.getReference("messages").child(chatId).push()
                        val msg = ChatMessage(senderId = myUid, text = messageText.trim(), timestamp = System.currentTimeMillis(), status = "SENT")
                        ref.setValue(msg)
                        updateRecentChat(db, myUid, otherUserId, isGroup, messageText.trim(), msg.timestamp)
                        messageText = ""
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == myUid
                    AnimatedChatBubble(msg = msg, isMe = isMe, onLongPress = {
                        selectedMessage = msg
                        showActionMenu = true
                    })
                }
            }
            if (isUploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter), color = BrandBlue)
            }
        }
        
        if (showActionMenu && selectedMessage != null) {
            AlertDialog(
                onDismissRequest = { showActionMenu = false },
                title = { Text("Message Options") },
                text = { Text("Do you want to delete this message?") },
                confirmButton = {
                    TextButton(onClick = {
                        db.getReference("messages").child(chatId).child(selectedMessage!!.id).removeValue()
                        showActionMenu = false
                    }) { Text("Delete", color = ErrorColor) }
                },
                dismissButton = {
                    TextButton(onClick = { showActionMenu = false }) { Text("Cancel") }
                }
            )
        }
    }
}

fun updateRecentChat(db: FirebaseDatabase, myUid: String, otherUserId: String, isGroup: Boolean, lastMsg: String, ts: Long) {
    if (isGroup) {
        // Group updates handled differently, ignored for now
    } else {
        val updates = mapOf(
            "chats/$myUid/$otherUserId/lastMessage" to lastMsg,
            "chats/$myUid/$otherUserId/timestamp" to ts,
            "chats/$otherUserId/$myUid/lastMessage" to lastMsg,
            "chats/$otherUserId/$myUid/timestamp" to ts
        )
        db.reference.updateChildren(updates)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedChatBubble(msg: ChatMessage, isMe: Boolean, onLongPress: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    Box(modifier = Modifier.scale(scale).combinedClickable(onClick = {}, onLongClick = onLongPress)) {
        ChatBubble(msg = msg, isMe = isMe)
    }
}

@Composable
fun ChatTopBar(name: String, profilePicture: String, otherUserId: String, isGroup: Boolean, onBack: () -> Unit, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .padding(top = 40.dp, bottom = 12.dp, start = 8.dp, end = 8.dp)
            .shadow(elevation = 2.dp, spotColor = Color.Black.copy(alpha = 0.03f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
        
        val bitmap = remember(profilePicture) {
            if (profilePicture.isNotEmpty()) {
                try {
                    val bytes = Base64.decode(profilePicture, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) { null }
            } else null
        }

        Box(modifier = Modifier.clickable { /* Show Profile Dialog */ }) {
            if (bitmap != null) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(42.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(if (isGroup) BrandPurple else BrandBlue), contentAlignment = Alignment.Center) {
                    Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(if (isGroup) "Group Chat" else "Online", style = MaterialTheme.typography.bodySmall, color = BrandBlue)
        }
        
        if (!isGroup) {
            AndroidView(
                modifier = Modifier.size(36.dp),
                factory = { ctx -> ZegoSendCallInvitationButton(ctx).apply { setIsVideoCall(true); setInvitees(listOf(com.zegocloud.uikit.service.defines.ZegoUIKitUser(otherUserId, name))) } }
            )
            Spacer(modifier = Modifier.width(8.dp))
            AndroidView(
                modifier = Modifier.size(36.dp),
                factory = { ctx -> ZegoSendCallInvitationButton(ctx).apply { setIsVideoCall(false); setInvitees(listOf(com.zegocloud.uikit.service.defines.ZegoUIKitUser(otherUserId, name))) } }
            )
        }
        IconButton(onClick = onSettingsClick) { Icon(Icons.Outlined.MoreVert, null, tint = TextPrimary) }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage, isMe: Boolean) {
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isMe) TextPrimary else SurfaceWhite
    val textColor = if (isMe) Color.White else TextPrimary
    val shape = if (isMe) RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = alignment) {
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .shadow(elevation = if (isMe) 0.dp else 2.dp, shape = shape, spotColor = Color.Black.copy(alpha = 0.05f))
                    .background(bgColor, shape)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (msg.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = msg.imageUrl,
                        contentDescription = "Image",
                        modifier = Modifier.width(200.dp).heightIn(max = 300.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else if (msg.audioUrl.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, null, tint = textColor, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.width(100.dp).height(4.dp).background(Color.Gray.copy(alpha=0.3f)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("0:05", color = textColor)
                    }
                } else {
                    Text(msg.text, color = textColor, fontSize = 15.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("10:00 AM", fontSize = 11.sp, color = TextSecondary)
                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    val tickColor = if (msg.status == "READ") BrandBlue else TextHint
                    Icon(if (msg.status == "SENT") Icons.Default.Check else Icons.Default.DoneAll, null, tint = tickColor, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(value: String, onValueChange: (String) -> Unit, onAttachClick: () -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceGray).clickable(onClick = onAttachClick),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Add, contentDescription = "Attach", tint = TextPrimary, modifier = Modifier.size(24.dp)) }

        Spacer(modifier = Modifier.width(12.dp))

        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp, max = 120.dp),
            placeholder = { Text("Type messages...", color = TextHint) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = InputBg,
                unfocusedContainerColor = InputBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
            ),
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        if (value.isNotBlank()) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(BrandBlue).clickable(onClick = onSend),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp)) }
        } else {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceGray).clickable { /* Record VN */ },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Mic, contentDescription = "Voice Note", tint = TextPrimary, modifier = Modifier.size(24.dp)) }
        }
    }
}
