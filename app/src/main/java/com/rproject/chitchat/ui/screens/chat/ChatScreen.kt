package com.rproject.chitchat.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.rproject.chitchat.ui.screens.home.AvatarImage
import com.rproject.chitchat.ui.screens.home.UserProfile
import com.rproject.chitchat.ui.screens.home.formatTimestamp
import com.rproject.chitchat.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(otherUserId: String, onBack: () -> Unit) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val chatId = if (currentUid < otherUserId) "${currentUid}_${otherUserId}" else "${otherUserId}_${currentUid}"

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var otherUser by remember { mutableStateOf<UserProfile?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Load other user info
    LaunchedEffect(otherUserId) {
        FirebaseDatabase.getInstance().getReference("users").child(otherUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    otherUser = snapshot.getValue(UserProfile::class.java)?.copy(uid = otherUserId)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // Load messages
    LaunchedEffect(chatId) {
        FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<ChatMessage>()
                    for (child in snapshot.children) {
                        val msg = child.getValue(ChatMessage::class.java)
                        if (msg != null) list.add(msg)
                    }
                    messages = list.sortedBy { it.timestamp }
                    if (list.isNotEmpty()) {
                        scope.launch { listState.animateScrollToItem(list.size - 1) }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    Scaffold(
        containerColor = ChitchatBgDark,
        topBar = {
            // Custom top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChitchatSurfaceDark)
                    .border(
                        width = 1.dp,
                        color = ChitchatOutline,
                        shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = ChitchatOnSurface)
                    }
                    AvatarImage(
                        profilePicture = otherUser?.profilePicture ?: "",
                        name = otherUser?.name ?: "",
                        size = 40
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            otherUser?.name ?: "Loading...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ChitchatOnSurface
                        )
                        Text(
                            otherUser?.phoneNumber ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChitchatOnSurfaceVariant
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChitchatSurfaceDark)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(ChitchatSurface2Dark)
                        .border(1.dp, ChitchatOutline, RoundedCornerShape(24.dp))
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tulis pesan...", color = ChitchatOnSurfaceVariant.copy(0.5f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = ChitchatOnSurface,
                            unfocusedTextColor = ChitchatOnSurface,
                            cursorColor = ChitchatPurple
                        ),
                        maxLines = 4
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank())
                                Brush.linearGradient(listOf(ChitchatPurple, ChitchatPurpleDark))
                            else
                                Brush.linearGradient(listOf(ChitchatOutline, ChitchatOutline))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val msgRef = FirebaseDatabase.getInstance()
                                    .getReference("chats").child(chatId).child("messages").push()
                                val msg = ChatMessage(
                                    senderId = currentUid,
                                    text = inputText.trim(),
                                    timestamp = System.currentTimeMillis()
                                )
                                msgRef.setValue(msg)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Kirim",
                            tint = if (inputText.isNotBlank()) Color.White else ChitchatOnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 12.dp, end = 12.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.senderId == currentUid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp, topEnd = 18.dp,
                                    bottomStart = if (isMe) 18.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 18.dp
                                )
                            )
                            .background(
                                if (isMe)
                                    Brush.linearGradient(listOf(ChitchatPurple, ChitchatPurpleDark))
                                else
                                    Brush.linearGradient(listOf(ChitchatSurface2Dark, ChitchatSurface2Dark))
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            msg.text,
                            color = if (isMe) Color.White else ChitchatOnSurface,
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        formatTimestamp(msg.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ChitchatOnSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
