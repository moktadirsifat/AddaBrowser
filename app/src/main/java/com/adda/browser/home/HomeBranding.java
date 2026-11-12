package com.adda.browser.home;

/** Holds the SIFAT logo, reflection, Bengali slogan and attribution styles. */
public final class HomeBranding {
    private HomeBranding() {}

    public static String getCss() {
        return
        ".logo-stage{position:relative;display:inline-block;margin:0 auto 34px;padding-bottom:30px;isolation:isolate}" +
        ".brand{position:relative;font-family:'Arial Black',Roboto,sans-serif;font-size:52px;font-style:italic;font-weight:900;letter-spacing:7px;line-height:1.2;background:linear-gradient(35deg,#e52b36 4%,#ff8a24 30%,#fff 57%,#3d8ff5 88%);-webkit-background-clip:text;background-clip:text;color:transparent;-webkit-text-fill-color:transparent;filter:drop-shadow(0 4px 3px rgba(14,42,55,.55)) drop-shadow(0 0 9px rgba(255,255,255,.24))}" +
        ".brand:after{content:attr(data-text);position:absolute;left:0;right:0;top:calc(100% + 1px);transform:scaleY(-1);transform-origin:top;background:linear-gradient(35deg,rgba(229,43,54,.58) 4%,rgba(255,138,36,.56) 30%,rgba(255,255,255,.52) 57%,rgba(61,143,245,.58) 88%);-webkit-background-clip:text;background-clip:text;color:transparent;-webkit-text-fill-color:transparent;opacity:.48;filter:blur(.7px);-webkit-mask-image:linear-gradient(to bottom,rgba(0,0,0,.7),transparent 82%);mask-image:linear-gradient(to bottom,rgba(0,0,0,.7),transparent 82%);animation:water-shimmer 3.8s ease-in-out infinite}" +
        ".waterline{position:absolute;left:-8%;right:-8%;bottom:12px;height:12px;border-radius:50%;background:linear-gradient(90deg,transparent,rgba(223,249,255,.58),rgba(255,255,255,.82),rgba(184,235,246,.55),transparent);filter:blur(3px);opacity:.75;animation:ripple 3.8s ease-in-out infinite}" +
        "@keyframes water-shimmer{0%,100%{opacity:.35;transform:scaleY(-1) skewX(0deg)}50%{opacity:.52;transform:scaleY(-1) skewX(2deg) translateY(2px)}}@keyframes ripple{0%,100%{transform:scaleX(.88);opacity:.45}50%{transform:scaleX(1.08);opacity:.85}}" +
        ".tag{font-family:'Galada','Noto Serif Bengali','Noto Sans Bengali',serif;font-size:20px;font-weight:400;margin-top:15px;padding:5px 2px;color:#fff;letter-spacing:.15px;text-align:center;background:transparent;border:0;box-shadow:none;text-shadow:0 2px 7px rgba(10,38,48,.72),0 0 13px rgba(255,255,255,.3)}" +
        ".foot{position:fixed;right:17px;bottom:15px;z-index:3;margin:0;text-align:left;line-height:1.45;letter-spacing:.5px;font-size:9px;color:#fff;text-shadow:0 1px 5px rgba(0,0,0,.55)}" +
        ".foot .by{display:block;color:#fff;font-family:Roboto,Arial,sans-serif;font-size:9px;font-weight:600;letter-spacing:1.4px;text-transform:uppercase;text-shadow:0 1px 5px rgba(0,0,0,.65)}" +
        ".foot .name{display:block;color:#fff;font-family:Roboto,Arial,sans-serif;font-size:10px;font-style:italic;font-weight:700;letter-spacing:.7px;text-shadow:0 1px 5px rgba(0,0,0,.65)}" +
    }
}
