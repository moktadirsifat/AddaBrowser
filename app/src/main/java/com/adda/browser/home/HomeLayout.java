package com.adda.browser.home;

/** Owns the home screen's content layout, search placement and shortcut grid styling. */
public final class HomeLayout {
    private HomeLayout() {}

    public static String getCss() {
        return
        ".content{position:relative;z-index:1;min-height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:30px 22px 76px}" +
        ".search{width:100%;max-width:440px;margin-top:42px;background:rgba(255,255,255,.91);border:1px solid rgba(255,255,255,.65);border-radius:18px;padding:7px 8px 7px 15px;display:flex;align-items:center;box-shadow:0 9px 28px rgba(0,0,0,.16)}" +
        ".g{font-size:22px;font-weight:800;background:conic-gradient(from -45deg,#4285f4 0 25%,#34a853 25% 48%,#fbbc05 48% 70%,#ea4335 70% 88%,#4285f4 88%);-webkit-background-clip:text;background-clip:text;color:transparent;margin-right:10px}" +
        ".search input{flex:1;min-width:0;border:0;outline:0;background:transparent;color:#27343a;font-size:15px;padding:11px 2px}" +
        ".search button{border:0;border-radius:13px;background:#dce6e8;color:#34464c;font-size:18px;width:43px;height:42px}" +
        ".shortcuts{display:flex;gap:13px;flex-wrap:wrap;justify-content:center;margin-top:31px;max-width:430px}" +
        ".shortcut{width:67px;text-decoration:none;color:#fff;text-align:center;font-size:11px;text-shadow:0 1px 5px rgba(0,0,0,.8)}" +
        ".bubble{width:48px;height:48px;margin:0 auto 8px;border-radius:16px;background:rgba(255,255,255,.96);color:#34464c;display:flex;align-items:center;justify-content:center;font-size:19px;font-weight:700;box-shadow:0 5px 15px rgba(0,0,0,.12)}.bubble svg{width:29px;height:29px;display:block}.bubble.youtube{color:#ff0033}.bubble.facebook{color:#0866ff}.bubble.wiki{color:#202124}.bubble.gmail{color:#ea4335}" +
        "@media(max-height:620px){.content{justify-content:flex-start;padding-top:22px}.search{margin-top:22px}.shortcuts{margin-top:16px}}" +;
    }
}
