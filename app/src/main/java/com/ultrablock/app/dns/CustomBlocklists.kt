package com.ultrablock.app.dns

object CustomBlocklists {

    // Complete list of WPS / Kingsoft domains, telemetry and cloud servers
    val WPS_DOMAINS = setOf(
        "wps.com",
        "wps.cn",
        "wpscdn.com",
        "wpscdn.cn",
        "kingsoft.com",
        "kingsoft-office-service.com",
        "ksosoft.com",
        "kmoffice.cn",
        "4wps.net",
        "docer.com",
        "docer.wps.cn",
        "mobileservice.wps.cn",
        "cloudservice.wps.cn",
        "account.wps.com",
        "drive.wps.com",
        "api.wps.com",
        "abroad-ad.kingsoft-office-service.com",
        "abroadad.cache.wpscdn.com",
        "ad.wps.cn",
        "adm.zookingsoft.com",
        "api-ad-adapter.wps.com",
        "api-ad-adapter-mb.wps.com",
        "api-ad-adapter-us.wps.com",
        "api-ad-adapter-sg.wps.com",
        "api-ad-bidding-ff.4wps.net",
        "statistic.ksosoft.com",
        "stat.wps.cn",
        "analytics.wps.com",
        "log.wps.cn",
        "feedback.wps.cn"
    )

    // Smart TV ACR, Sony Bravia Telemetry & Sponsored Recommendations
    val SMART_TV_DOMAINS = setOf(
        // Samba TV (Automatic Content Recognition - ACR)
        "samba.tv",
        "tv.samba.tv",
        "api.samba.tv",
        "config.samba.tv",
        "events.samba.tv",
        "optout.samba.tv",

        // Sony Bravia Telemetry & Sponsored Rows
        "sonyselect.sony.net",
        "bravia.sony.tv",
        "bravia-ad.sony.com",
        "snei.sony.net",
        "bravia-cloud.sony.net",
        "ssm.samba.tv"
    )

    // Commercial Mobile App Trackers & Telemetry SDKs
    val APP_TRACKERS_DOMAINS = setOf(
        // AppsFlyer
        "appsflyer.com",
        "t.appsflyer.com",
        "app.appsflyer.com",
        "events.appsflyer.com",
        "conversions.appsflyer.com",
        "register.appsflyer.com",
        "hq1.appsflyer.com",
        "gcdsdk.appsflyer.com",
        "onelink.me",

        // CleverTap & WizRocket
        "clevertap.com",
        "wzrkt.com",
        "spiky.clevertap.com",
        "in1.clevertap-prod.com",
        "eu1.clevertap-prod.com",
        "us1.clevertap-prod.com",

        // MoEngage
        "moengage.com",
        "app-analytics.moengage.com",
        "sdk-01.moengage.com",
        "sdk-02.moengage.com",
        "sdk-03.moengage.com",
        "livecounter.moengage.com",

        // Adjust
        "adjust.com",
        "app.adjust.com",
        "control.adjust.com",
        "event.adjust.com",

        // Branch.io
        "branch.io",
        "api.branch.io",
        "api2.branch.io",
        "app.link",
        "bnc.lt",

        // Mixpanel & Singular & Kochava
        "mixpanel.com",
        "api.mixpanel.com",
        "singular.net",
        "kochava.com",
        "control.kochava.com"
    )

    // Major Mobile & TV In-App Ad Networks (Google DoubleClick/AdMob, Vungle, AppLovin, IronSource)
    val INAPP_ADS_DOMAINS = setOf(
        // Vungle Ads
        "vungle.com",
        "api.vungle.com",
        "ads.vungle.com",
        "events.vungle.com",

        // AppLovin
        "applovin.com",
        "applvn.com",
        "d.applovin.com",
        "a.applovin.com",
        "rt.applovin.com",
        "ms.applovin.com",

        // IronSource
        "ironsrc.mobi",
        "supersonicads.com",
        "sonic-eu.supersonicads.com",
        "is.com",

        // InMobi
        "inmobi.com",
        "inmobicdn.net",
        "config.inmobi.com",
        "telemetry.inmobi.com",

        // Amazon Ad System
        "amazon-adsystem.com",
        "aax.amazon-adsystem.com",
        "aax-us-east.amazon-adsystem.com",
        "aax-fe.amazon-adsystem.com",
        "fls-na.amazon.com",

        // Unity Ads & Criteo
        "unityads.unity3d.com",
        "auction.unityads.unity3d.com",
        "criteo.com",
        "criteo.net"
    )

    // Realme / Oppo / ColorOS Cloud & Telemetry endpoints
    val REALME_OPPO_DOMAINS = setOf(
        "heytap.com",
        "heytapimage.com",
        "heytapmobile.com",
        "heytapdl.com",
        "oppo.com",
        "coloros.com",
        "realme.com",
        "realmemobile.com",
        "finshellpay.com",
        "nearme.com.cn",
        "oplusmobile.com"
    )

    // Xiaomi / MIUI / HyperOS Telemetry & Ad services
    val XIAOMI_DOMAINS = setOf(
        "miui.com",
        "xiaomi.com",
        "ad.xiaomi.com",
        "tracking.miui.com",
        "data.mistat.xiaomi.com",
        "analytics.mobile.xiaomi.net"
    )

    // Facebook / Meta tracking, SDKs and Graph APIs
    val META_DOMAINS = setOf(
        "facebook.com",
        "facebook.net",
        "fbcdn.net",
        "fbsbx.com",
        "graph.facebook.com",
        "connect.facebook.net",
        "meta.com"
    )

    // ByteDance / TikTok Tracking & Ads
    val BYTEDANCE_DOMAINS = setOf(
        "byteoversea.com",
        "ibytedtos.com",
        "tiktokcdn.com",
        "musical.ly",
        "pstatp.com"
    )
}
