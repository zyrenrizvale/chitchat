package com.rproject.chitchat.ui.screens.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.rproject.chitchat.ui.components.AuthLayout
import com.rproject.chitchat.ui.theme.*
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(onSetupComplete: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    AuthLayout(
        headerContent = {
            // Avatar picker in header
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(3.dp, Color.White, CircleShape)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Foto profil",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                // Camera badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, BrandBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        bodyContent = {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Complete Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Name input
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(28.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = InputBg,
                    unfocusedContainerColor = InputBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                shape = RoundedCornerShape(28.dp),
                placeholder = {
                    Text("Full Name", color = TextHint)
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = {
                    if (name.isNotBlank()) {
                        isLoading = true
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null) {
                            var base64Image = ""
                            if (imageUri != null) {
                                try {
                                    val inputStream = context.contentResolver.openInputStream(imageUri!!)
                                    val bitmap = BitmapFactory.decodeStream(inputStream)
                                    val resized = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
                                    val baos = ByteArrayOutputStream()
                                    resized.compress(Bitmap.CompressFormat.JPEG, 60, baos)
                                    base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                            val userMap = mapOf(
                                "name" to name,
                                "profilePicture" to base64Image,
                                "phoneNumber" to (FirebaseAuth.getInstance().currentUser?.phoneNumber ?: "")
                            )
                            FirebaseDatabase.getInstance().getReference("users").child(uid)
                                .setValue(userMap)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) onSetupComplete()
                                    else Toast.makeText(context, "Gagal menyimpan profil", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.5.dp, BrandBlue),
                enabled = name.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BrandBlue, strokeWidth = 2.dp)
                } else {
                    Text("Finish Setup", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = BrandBlue)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    )
}
