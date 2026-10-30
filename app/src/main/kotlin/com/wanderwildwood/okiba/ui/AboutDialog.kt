package com.wanderwildwood.okiba.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.okiba.BuildConfig
import com.wanderwildwood.okiba.R

/** What this is, what it keeps and uses, its terms and licence, and where its source is. */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var licences by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    // The link opens through the shared guarded opener: a real browser, or the plain message.
    fun open(url: String, shown: String) {
        openLink(context, url, context.getString(R.string.about_no_browser_address, shown))
    }

    // Back in the Licences page returns to the About rather than leaving both.
    EInkDialog(onDismiss = { if (licences) licences = false else onDismiss() }) {
        if (licences) {
            val licenceRoom = (LocalConfiguration.current.screenHeightDp - 165).coerceIn(120, 420).dp
            LicencesPage(onBack = { licences = false }, room = licenceRoom)
            return@EInkDialog
        }
        TextMMD(
            text = stringResource(R.string.about_title, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )

        Spacer(Modifier.height(10.dp))
        // The body pages on whole rows; the Close button stays outside it, always in reach.
        val room = (LocalConfiguration.current.screenHeightDp - 165).coerceIn(120, 420).dp
        PagedColumn(Modifier.fillMaxWidth().height(room)) {
            item { TextMMD(text = stringResource(R.string.about_what), style = MaterialTheme.typography.bodyMedium) }
            item { Heading(stringResource(R.string.about_privacy_title)) }
            item { TextMMD(text = stringResource(R.string.about_privacy), style = MaterialTheme.typography.bodyMedium) }
            item { TextMMD(text = stringResource(R.string.about_privacy_more), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
            item { TextMMD(text = stringResource(R.string.about_privacy_spots), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
            item { TextMMD(text = stringResource(R.string.about_privacy_meter), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
            item { Heading(stringResource(R.string.about_terms_title)) }
            item { TextMMD(text = stringResource(R.string.about_terms), style = MaterialTheme.typography.bodyMedium) }
            item {
                Column(Modifier.padding(top = 12.dp)) {
                    TextMMD(text = stringResource(R.string.about_licence), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                TextMMD(
                    text = stringResource(R.string.about_licences),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().clickable { licences = true }.heightIn(min = 48.dp).wrapContentHeight(Alignment.CenterVertically),
                )
            }
            item {
                // The source opens in the browser, the one press here that leaves the app.
                TextMMD(
                    text = SOURCE,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { open("https://$SOURCE", SOURCE) }
                        .wrapContentHeight(Alignment.CenterVertically),
                )
            }
            item { Llama(::open) }
        }

        Spacer(Modifier.height(14.dp))
        OutlinedButtonMMD(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { TextMMD(text = stringResource(R.string.about_close), style = MaterialTheme.typography.bodySmall) }
    }
}

private const val SOURCE = "github.com/wanderwildwood/okiba"

/** A section's name in the About: bold, a little apart from what came before. */
@Composable
private fun Heading(text: String) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
    )
}

/**
 * A llama at the foot of the About, which opens the page a donation goes to. The site's
 * address sits at the start of the same line and opens the site; the llama and its words
 * open the page.
 *
 * Straight to the checkout: the Donate button on the site only leads there anyway. The short
 * square.link form, which is what the site itself links to, so a regenerated checkout follows
 * it. A phone with nothing that opens a web address says so rather than doing nothing.
 */
@Composable
private fun Llama(open: (String, String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextMMD(
            text = "wanderthe.dev",
            style = MaterialTheme.typography.bodyMedium,
            // The site's address opens the site, the way the llama beside it opens its page.
            modifier = Modifier
                .clickable { open("https://wanderthe.dev", "wanderthe.dev") }
                .heightIn(min = 48.dp)
                .padding(end = 4.dp)
                .wrapContentHeight(Alignment.CenterVertically),
        )
        Spacer(Modifier.width(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { open("https://square.link/u/AGu8oT10", "square.link/u/AGu8oT10") }
                .heightIn(min = 48.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.llama),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(6.dp))
            TextMMD(text = stringResource(R.string.about_feed_the_llamas), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
