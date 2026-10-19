package com.nutriai.mvp.runtime

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Opens only local files/content URIs; there is no remote image loader in the APK. */
@Composable
fun LocalPhoto(path: String, description: String, modifier: Modifier = Modifier) {
    val context=LocalContext.current
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(path) {
        bitmap=withContext(Dispatchers.IO) { runCatching {
            fun stream()=if(path.startsWith("content:") || path.startsWith("file:")) context.contentResolver.openInputStream(Uri.parse(path)) else File(path).inputStream()
            val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
            stream()?.use { BitmapFactory.decodeStream(it,null,bounds) }
            var sample=1
            while(bounds.outWidth/sample>1200 || bounds.outHeight/sample>1200) sample*=2
            stream()?.use { BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply { inSampleSize=sample }) }
        }.getOrNull() }
    }
    bitmap?.let { Image(it.asImageBitmap(),description,modifier,contentScale=ContentScale.Fit) }
}
