package com.adda.browser.home;

/** Selectable, lightweight home backgrounds and their motion controls. */
public final class HomeBackground {
    private HomeBackground() {}

    public static String getCss() {
        return
        ".scene{transition:background 450ms ease}" +
        "body[data-theme='scenic'] .scene{background:linear-gradient(180deg,#8fc5df 0%,#d5d9ce 43%,#4d9db5 64%,#15536d 100%)}" +
        "body[data-theme='aurora'] .scene{background:linear-gradient(180deg,#071a36 0%,#123e5a 52%,#1d6b70 100%)}" +
        "body[data-theme='night'] .scene{background:radial-gradient(ellipse at 50% 78%,#263f72 0%,#111c3c 48%,#070d20 100%)}" +
        "body[data-theme='forest'] .scene{background:linear-gradient(180deg,#b3d6bd 0%,#5c9b79 48%,#173f37 100%)}" +
        "body[data-theme='aurora'] .scene:before{content:'';position:absolute;inset:-25%;background:linear-gradient(115deg,transparent 25%,rgba(73,244,190,.12) 38%,rgba(93,178,255,.26) 48%,rgba(174,121,255,.16) 57%,transparent 70%);filter:blur(24px);animation:aurora-drift 13s ease-in-out infinite alternate}" +
        "body[data-theme='night'] .scene:after{content:'';position:absolute;inset:0;background-image:radial-gradient(2px 2px at 12% 18%,#fff 50%,transparent 70%),radial-gradient(1.5px 1.5px at 32% 42%,#fff 50%,transparent 70%),radial-gradient(2px 2px at 73% 22%,#fff 50%,transparent 70%),radial-gradient(1.5px 1.5px at 86% 54%,#fff 50%,transparent 70%),radial-gradient(2px 2px at 55% 12%,#fff 50%,transparent 70%);animation:star-twinkle 3.8s ease-in-out infinite alternate}" +
        "body[data-theme='aurora'] .scene svg,body[data-theme='night'] .scene svg,body[data-theme='forest'] .scene svg{opacity:0}" +
        ".theme-switcher{position:fixed;z-index:5;top:12px;right:12px;display:flex;gap:6px;padding:6px;border-radius:18px;background:rgba(9,28,38,.34);backdrop-filter:blur(8px)}" +
        ".theme-choice{width:29px;height:29px;border:2px solid rgba(255,255,255,.72);border-radius:50%;padding:0;box-shadow:0 2px 7px rgba(0,0,0,.2)}" +
        ".theme-choice[aria-pressed='true']{outline:2px solid #fff;outline-offset:2px}" +
        ".theme-choice[data-choice='scenic']{background:linear-gradient(145deg,#9bd5e8 0 48%,#286f69 49%)}" +
        ".theme-choice[data-choice='aurora']{background:linear-gradient(145deg,#173d6a,#54d6ae,#6d62b8)}" +
        ".theme-choice[data-choice='night']{background:linear-gradient(145deg,#080f29,#596da9)}" +
        ".theme-choice[data-choice='forest']{background:linear-gradient(145deg,#b7d7a0,#24563e)}" +
        ".motion-leaves{position:fixed;inset:0;z-index:2;pointer-events:none;overflow:hidden;opacity:0}" +
        ".motion-clouds{position:fixed;inset:0;z-index:1;pointer-events:none;overflow:hidden;opacity:0}" +
        "body[data-theme='scenic'] .motion-clouds{opacity:1}" +
        ".cloud{--cloud-scale:1;position:absolute;left:-32%;width:clamp(110px,24vw,230px);height:34px;border-radius:50px;background:rgba(255,255,255,.42);filter:blur(1.5px);animation:cloud-drift 58s linear infinite}" +
        ".cloud:before,.cloud:after{content:'';position:absolute;bottom:7px;border-radius:50%;background:inherit}" +
        ".cloud:before{left:22%;width:42%;height:48px}.cloud:after{left:48%;width:32%;height:36px}" +
        ".cloud.c2{top:25%;left:-48%;opacity:.68;transform:scale(.72);animation-duration:78s;animation-delay:-31s}" +
        ".cloud.c3{top:39%;left:-36%;opacity:.48;transform:scale(.52);animation-duration:92s;animation-delay:-57s}" +
        "@keyframes cloud-drift{from{transform:translate3d(0,0,0) scale(var(--cloud-scale))}to{transform:translate3d(150vw,0,0) scale(var(--cloud-scale))}}" +
        "body[data-theme='forest'] .motion-leaves{opacity:1}" +
        ".leaf{position:absolute;top:-8%;left:18%;width:13px;height:20px;border-radius:100% 0 100% 0;background:rgba(220,246,184,.72);filter:drop-shadow(0 1px 2px rgba(0,0,0,.18));animation:leaf-drift 12s linear infinite}" +
        ".leaf:nth-child(2){left:54%;animation-delay:-5s;animation-duration:15s;transform:scale(.75)}.leaf:nth-child(3){left:82%;animation-delay:-9s;animation-duration:13s;transform:scale(.6)}" +
        "@keyframes aurora-drift{0%{transform:translate3d(-5%,2%,0) rotate(-8deg)}100%{transform:translate3d(8%,-3%,0) rotate(8deg)}}" +
        "@keyframes star-twinkle{0%{opacity:.45}100%{opacity:1}}" +
        "@keyframes leaf-drift{0%{transform:translate3d(0,-5vh,0) rotate(0deg)}50%{transform:translate3d(24px,52vh,0) rotate(170deg)}100%{transform:translate3d(-18px,112vh,0) rotate(340deg)}}" +
        "@media(prefers-reduced-motion:reduce){.scene,.leaf,.cloud,.scene:before,.scene:after,.brand:after,.waterline{animation:none!important;transition:none!important}.birds{display:none!important}}" +
        "body.reduce-motion .scene,body.reduce-motion .leaf,body.reduce-motion .cloud,body.reduce-motion .scene:before,body.reduce-motion .scene:after,body.reduce-motion .brand:after,body.reduce-motion .waterline{animation:none!important;transition:none!important}body.reduce-motion .birds{display:none!important}";
    }

    public static String getMarkup() {
        return
        "<div class='theme-switcher' role='group' aria-label='Choose background'>" +
        "<button class='theme-choice' type='button' data-choice='scenic' aria-label='Scenic background' aria-pressed='true'></button>" +
        "<button class='theme-choice' type='button' data-choice='aurora' aria-label='Aurora background' aria-pressed='false'></button>" +
        "<button class='theme-choice' type='button' data-choice='night' aria-label='Night sky background' aria-pressed='false'></button>" +
        "<button class='theme-choice' type='button' data-choice='forest' aria-label='Forest background' aria-pressed='false'></button>" +
        "</div><div class='motion-clouds' aria-hidden='true'><i class='cloud' style='top:13%'></i><i class='cloud c2'></i><i class='cloud c3' style='top:52%'></i></div>" +
        "<div class='motion-leaves' aria-hidden='true'><i class='leaf'></i><i class='leaf'></i><i class='leaf'></i></div>";
    }

    public static String getScript() {
        return
        "(()=>{const body=document.body;const buttons=[...document.querySelectorAll('.theme-choice')];" +
        "const valid=['scenic','aurora','night','forest'];let saved='scenic';" +
        "try{const value=localStorage.getItem('sifat-home-theme');if(valid.includes(value))saved=value;}catch(e){}" +
        "function setTheme(name){if(!valid.includes(name))return;body.dataset.theme=name;" +
        "buttons.forEach(button=>button.setAttribute('aria-pressed',String(button.dataset.choice===name)));" +
        "try{localStorage.setItem('sifat-home-theme',name);}catch(e){}}" +
        "buttons.forEach(button=>button.addEventListener('click',()=>setTheme(button.dataset.choice)));" +
        "setTheme(saved);const motion=window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches;" +
        "if(motion)body.classList.add('reduce-motion');})();";
    }
}
