package com.rproject.chitchat.ui.screens.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

@Composable
fun ProfileSetupScreen(onSetupComplete: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Setup Profil", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Pilih Foto")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nama Anda") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

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
                                val bytes = baos.toByteArray()
                                base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        val userMap = mapOf(
                            "name" to name,
                            "profilePicture" to base64Image,
                            "phoneNumber" to FirebaseAuth.getInstance().currentUser?.phoneNumber
                        )

                        FirebaseDatabase.getInstance().getReference("users").child(uid)
                            .setValue(userMap)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    onSetupComplete()
                                } else {
                                    Toast.makeText(context, "Gagal menyimpan profil", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Simpan Profil")
            }
        }
    }
}
