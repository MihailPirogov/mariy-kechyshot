package ru.mari.kechyshot

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.*
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.YandexAds
import com.yandex.mobile.ads.nativeads.*

/**
 * ЕДИНСТВЕННОЕ место с идентификаторами рекламных блоков Yandex Mobile Ads.
 * Сейчас стоят DEMO-идентификаторы. Чтобы включить реальную рекламу: partner.yandex.ru →
 * «Мобильные приложения» → добавить приложение ru.pirogovlab.mariykechyshot → создать блоки
 * «Нативный» и «Баннер (sticky)» → вставить их R-M-XXXXXX-Y сюда и пересобрать.
 */
object AdsConfig {
    val ENABLED = !BuildConfig.NO_ADS
    val NATIVE_TODAY = if (BuildConfig.DEBUG) "demo-native-content-yandex" else "R-M-20228039-1"
    val BANNER_MONTH = if (BuildConfig.DEBUG) "demo-banner-yandex" else "R-M-20228039-2"
}

object Ads {
    @Volatile private var inited = false
    fun init(ctx: Context) { if (!inited && AdsConfig.ENABLED) { inited = true; YandexAds.initialize(ctx.applicationContext) {} } }

    /** Native ad styled as an event card, labelled «Реклама». Adds itself to [container] when loaded. */
    fun nativeCard(a: Activity, container: LinearLayout, index: Int) {
        if (!AdsConfig.ENABLED) return
        NativeAdLoader(a).loadAd(AdRequest.Builder(AdsConfig.NATIVE_TODAY).build(), object : NativeAdLoadListener {
            override fun onAdLoaded(ad: NativeAd) { if (!a.isFinishing) runCatching { container.addView(buildCard(a, ad), index.coerceAtMost(container.childCount)) } }
            override fun onAdFailedToLoad(error: AdRequestError) { android.util.Log.i("MKAds", "native fail: ${error.code} ${error.description}") }
        })
    }

    private fun buildCard(c: Context, ad: NativeAd): View {
        val root = NativeAdView(c)
        val card = c.hbox().apply { background = c.rounded(SURFACE, 18); setPadding(0, 0, c.dp(12), 0) }
        val stripe = View(c).apply { background = c.rounded(0xFF8C7A70.toInt(), 3) }
        card.addView(stripe, LinearLayout.LayoutParams(c.dp(5), -1).apply { setMargins(0, c.dp(8), c.dp(10), c.dp(8)) })
        val body = c.vbox().apply { setPadding(0, c.dp(10), 0, c.dp(10)) }
        val top = c.hbox()
        val label = c.tv("Реклама", 11f, 0xFF8C7A70.toInt(), true)
        val sponsored = c.tv("", 11f, MUTED); val age = c.tv("", 11f, MUTED)
        val feedback = ImageView(c)
        top.addView(label); top.addView(View(c), LinearLayout.LayoutParams(c.dp(6), 1)); top.addView(sponsored, LinearLayout.LayoutParams(0, -2, 1f)); top.addView(age)
        top.addView(feedback, LinearLayout.LayoutParams(c.dp(20), c.dp(20)).apply { leftMargin = c.dp(6) })
        body.addView(top)
        val row = c.hbox()
        val icon = ImageView(c); row.addView(icon, LinearLayout.LayoutParams(c.dp(40), c.dp(40)).apply { rightMargin = c.dp(10) })
        val texts = c.vbox()
        val title = c.tv("", 16f, DARK, true, 2); val text = c.tv("", 13f, MUTED, false, 3); val domain = c.tv("", 12f, MUTED, false, 1)
        texts.addView(title); texts.addView(text); texts.addView(domain); row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        body.addView(row)
        val media = MediaView(c); body.addView(media, LinearLayout.LayoutParams(-1, -2).apply { topMargin = c.dp(6) })
        val warning = c.tv("", 11f, MUTED, false, 2); body.addView(warning)
        val cta = Button(c).apply { setTextColor(CREAM); background = c.rounded(RED, 18); textSize = 14f; isAllCaps = false }
        body.addView(cta, LinearLayout.LayoutParams(-2, c.dp(40)).apply { topMargin = c.dp(6) })
        val price = c.tv("", 12f, MUTED); val reviews = c.tv("", 12f, MUTED); val favicon = ImageView(c)
        card.addView(body, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(card)
        val binder = NativeAdViewBinder.Builder(root)
            .setAgeView(age).setBodyView(text).setCallToActionView(cta).setDomainView(domain).setFaviconView(favicon)
            .setFeedbackView(feedback).setIconView(icon).setMediaView(media).setPriceView(price).setReviewCountView(reviews)
            .setSponsoredView(sponsored).setTitleView(title).setWarningView(warning).build()
        ad.bindNativeAd(binder)
        root.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(c.dp(12), c.dp(10), c.dp(12), c.dp(4)) }
        applyFonts(root)
        return root
    }

    fun banner(a: Activity): View? {
        if (!AdsConfig.ENABLED) return null
        val v = BannerAdView(a)
        val wDp = (a.resources.displayMetrics.widthPixels / a.resources.displayMetrics.density).toInt()
        v.setAdSize(BannerAdSize.sticky(a, wDp))
        v.setBannerAdEventListener(object : BannerAdEventListener {
            override fun onAdLoaded() {}
            override fun onAdFailedToLoad(error: AdRequestError) { android.util.Log.i("MKAds", "banner fail: ${error.code} ${error.description}"); v.visibility = View.GONE }
            override fun onAdClicked() {}
            override fun onImpression(impressionData: ImpressionData?) {}
        })
        v.loadAd(AdRequest.Builder(AdsConfig.BANNER_MONTH).build())
        return v
    }
}
