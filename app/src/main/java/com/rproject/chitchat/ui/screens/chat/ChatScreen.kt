package com.rproject.chitchat.ui.screens.chat

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Videocam
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.rproject.chitchat.ui.theme.*

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(otherUserId: String, onBack: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var otherUserName by remember { mutableStateOf("User") }
    var otherUserImage by remember { mutableStateOf("") }

    val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseDatabase.getInstance()
    val chatId = if (myUid < otherUserId) "${myUid}_$otherUserId" else "${otherUserId}_$myUid"
    val listState = rememberLazyListState()

    // Fetch Other User Info
    LaunchedEffect(otherUserId) {
        db.getReference("users").child(otherUserId).get().addOnSuccessListener { s ->
            otherUserName = s.child("name").getValue(String::class.java) ?: "User"
            otherUserImage = s.child("profilePicture").getValue(String::class.java) ?: ""
        }
    }

    // Fetch Messages
    LaunchedEffect(chatId) {
        db.getReference("messages").child(chatId).orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    val list = mutableListOf<ChatMessage>()
                    for (m in s.children) {
                        m.getValue(ChatMessage::class.java)?.let { list.add(it.copy(id = m.key ?: "")) }
                    }
                    messages = list
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    // Auto-scroll to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            ChatTopBar(
                name = otherUserName,
                profilePicture = otherUserImage,
                onBack = onBack
            )
        },
        bottomBar = {
            ChatInputBar(
                value = messageText,
                onValueChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        val ref = db.getReference("messages").child(chatId).push()
                        val msg = ChatMessage(senderId = myUid, text = messageText.trim(), timestamp = System.currentTimeMillis())
                        ref.setValue(msg)

                        // Update recent chats
                        val updates = mapOf(
                            "chats/$myUid/$otherUserId/lastMessage" to messageText,
                            "chats/$myUid/$otherUserId/timestamp" to msg.timestamp,
                            "chats/$otherUserId/$myUid/lastMessage" to messageText,
                            "chats/$otherUserId/$myUid/timestamp" to msg.timestamp
                        )
                        db.reference.updateChildren(updates)
                        messageText = ""
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.senderId == myUid
                ChatBubble(msg = msg, isMe = isMe)
            }
        }
    }
}

@Composable
fun ChatTopBar(name: String, profilePicture: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .padding(top = 40.dp, bottom = 12.dp, start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        
        // Avatar
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
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(BrandBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))
        
        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
        
        IconButton(onClick = { }) { Icon(Icons.Outlined.Videocam, null, tint = TextPrimary) }
        IconButton(onClick = { }) { Icon(Icons.Outlined.Call, null, tint = TextPrimary) }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage, isMe: Boolean) {
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isMe) TextPrimary else SurfaceWhite
    val textColor = if (isMe) Color.White else TextPrimary
    val shape = if (isMe) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .shadow(elevation = if (isMe) 0.dp else 2.dp, shape = shape, spotColor = Color.Black.copy(alpha = 0.05f))
                    .background(bgColor, shape)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(msg.text, color = textColor, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("10:00 AM", fontSize = 11.sp, color = TextSecondary)
                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.DoneAll, null, tint = BrandBlue, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { }) {
            Icon(Icons.Default.Add, contentDescription = "Attach", tint = TextSecondary, modifier = Modifier.size(28.dp))
        }

        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp, max = 120.dp),
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

        Spacer(modifier = Modifier.width(8.dp))

        if (value.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandBlue)
                    .clickable { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        } else {
            IconButton(onClick = { }) {
                Icon(Icons.Outlined.Mic, contentDescription = "Voice Note", tint = TextSecondary)
            }
        }
    }
}
