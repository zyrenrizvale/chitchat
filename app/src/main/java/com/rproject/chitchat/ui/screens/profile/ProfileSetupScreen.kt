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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.rproject.chitchat.ui.theme.*
import java.io.ByteArrayOutputStream

@Composable
fun ProfileSetupScreen(onSetupComplete: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ChitchatBgDark, ChitchatSurfaceDark, Color(0xFF1A1830))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            Text(
                "Setup Profil",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold, color = ChitchatOnSurface
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Buat profil kamu agar teman\nbisa mengenali kamu",
                style = MaterialTheme.typography.bodyMedium,
                color = ChitchatOnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Avatar picker
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(ChitchatSurface2Dark)
                        .border(2.dp, ChitchatPurple, CircleShape)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("👤", fontSize = 42.sp)
                    }
                }
                // Camera badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(ChitchatPurple)
                        .border(2.dp, ChitchatBgDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Card form
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(ChitchatSurfaceDark.copy(alpha = 0.85f))
                    .border(1.dp, ChitchatOutline, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        "Nama Kamu",
                        style = MaterialTheme.typography.labelLarge,
                        color = ChitchatOnSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ChitchatSurface2Dark)
                            .border(
                                1.5.dp,
                                if (name.isNotEmpty()) ChitchatPurple else ChitchatOutline,
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = ChitchatOnSurface,
                                unfocusedTextColor = ChitchatOnSurface,
                                cursorColor = ChitchatPurple
                            ),
                            placeholder = {
                                Text("Masukkan nama kamu", color = ChitchatOnSurfaceVariant.copy(alpha = 0.5f))
                            },
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
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
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChitchatPurple),
                        shape = RoundedCornerShape(14.dp),
                        enabled = name.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                        } else {
                            Text("Simpan & Lanjutkan", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
