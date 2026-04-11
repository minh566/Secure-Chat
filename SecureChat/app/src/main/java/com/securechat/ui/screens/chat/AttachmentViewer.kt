package com.securechat.ui.screens.chat

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import com.securechat.domain.model.Message
import com.securechat.domain.model.MessageType
import java.io.File

@Composable
fun AttachmentViewerDialog(
    message: Message,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF10142A)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Dong",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = message.fileName ?: message.content,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1
                    )
                }

                when (message.type) {
                    MessageType.IMAGE -> ImageAttachmentViewer(message.viewerSource())
                    MessageType.FILE -> FileAttachmentViewer(message.viewerSource())
                    else -> ViewerError("Khong co du lieu de xem")
                }
            }
        }
    }
}

private fun Message.viewerSource(): String? {
    val local = localCachePath
    return if (!local.isNullOrBlank() && File(local).exists()) {
        Uri.fromFile(File(local)).toString()
    } else {
        fileUrl
    }
}

@Composable
private fun ImageAttachmentViewer(url: String?) {
    if (url.isNullOrBlank()) {
        ViewerError("Anh khong ton tai hoac da bi xoa")
        return
    }

    SubcomposeAsyncImage(
        model = url,
        contentDescription = "Anh",
        modifier = Modifier.fillMaxSize(),
        loading = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        },
        error = {
            ViewerError("Khong tai duoc anh. Tep co the da bi xoa")
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun FileAttachmentViewer(url: String?) {
    if (url.isNullOrBlank()) {
        ViewerError("Tep khong ton tai hoac da bi xoa")
        return
    }

    var isLoading by remember(url) { mutableStateOf(true) }
    var hasError by remember(url) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                hasError = true
                                isLoading = false
                            }
                        }
                    }
                    loadUrl(url)
                }
            },
            update = { webView ->
                if (webView.url != url) {
                    hasError = false
                    isLoading = true
                    webView.loadUrl(url)
                }
            }
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        if (hasError) {
            ViewerError("Khong preview duoc tep nay trong ung dung")
        }
    }
}

@Composable
private fun ViewerError(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF10142A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color(0xFFB6C0FF),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                color = Color(0xFFD1D6F9),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

