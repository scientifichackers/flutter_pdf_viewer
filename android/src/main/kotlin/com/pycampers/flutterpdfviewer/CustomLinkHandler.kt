package com.pycampers.flutterpdfviewer

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.link.LinkHandler
import com.github.barteksc.pdfviewer.model.LinkTapEvent


class CustomLinkHandler(val pdfView: PDFView) : LinkHandler {
    companion object {
        @JvmStatic
        val TAG: String = CustomLinkHandler::class.java.simpleName
    }

    override fun handleLinkEvent(event: LinkTapEvent) {
        val uri = event.link.uri
        val page = event.link.destPageIdx

        if (uri != null && !uri.isEmpty()) {
            handleUri(uri)
        } else if (page != null) {
            handlePage(page)
        }
    }

    private fun handleUri(uri: String) {
        val parsedUri = Uri.parse(uri)
        val intent = Intent(Intent.ACTION_VIEW, parsedUri)
        val context = pdfView.context

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Log.w(TAG, "No activity found for URI: $uri")
        }
    }

    private fun handlePage(page: Int) {
        pdfView.jumpTo(page)
    }
}