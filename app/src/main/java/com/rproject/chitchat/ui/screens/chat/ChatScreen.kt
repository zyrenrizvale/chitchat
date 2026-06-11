package com.rproject.chitchat.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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
    var otherUserName by remember { mutableStateOf("User") }

    LaunchedEffect(otherUserId) {
        FirebaseDatabase.getInstance().getReference("users").child(otherUserId)
            .child("name").get().addOnSuccessListener { snapshot ->
                otherUserName = snapshot.value as? String ?: "User"
            }
    }

    LaunchedEffect(chatId) {
        val dbRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val msg = child.getValue(ChatMessage::class.java)
                    if (msg != null) list.add(msg)
                }
                messages = list
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(otherUserName) })
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Tulis pesan...") }
                )
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val msgRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages").push()
                            val msg = ChatMessage(senderId = currentUid, text = inputText, timestamp = System.currentTimeMillis())
                            msgRef.setValue(msg)
                            inputText = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Kirim")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            items(messages) { msg ->
                val isMe = msg.senderId == currentUid
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Text(msg.text, color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
