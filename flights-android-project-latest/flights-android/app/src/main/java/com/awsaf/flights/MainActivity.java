package com.awsaf.flights;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.webkit.*;
public class MainActivity extends Activity {
  private WebView web;
  private void ext(Uri u) { try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) { } }
  class Bridge {
    @JavascriptInterface public void check(final String csv) {
      new Thread(new Runnable() { public void run() {
        org.json.JSONObject out = new org.json.JSONObject();
        int okc = 0;
        String[] bases = { "https://api.adsb.lol/v2/callsign/", "https://opendata.adsb.fi/api/v2/callsign/" };
        for (String cs : csv.split(",")) {
          if (cs.length() < 4) continue;
          for (String base : bases) {
            try {
              java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(base + cs).openConnection();
              c.setConnectTimeout(8000); c.setReadTimeout(8000);
              c.setRequestProperty("User-Agent", "FlightsApp/1.0");
              java.io.InputStream in = c.getInputStream();
              java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
              byte[] buf = new byte[4096]; int n;
              while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
              org.json.JSONObject j = new org.json.JSONObject(bo.toString("UTF-8"));
              okc++;
              org.json.JSONArray ac = j.optJSONArray("ac");
              if (ac != null && ac.length() > 0) {
                org.json.JSONObject pick = ac.getJSONObject(0);
                for (int i = 0; i < ac.length(); i++) { org.json.JSONObject a = ac.getJSONObject(i); if (!"ground".equals(a.optString("alt_baro", "ground"))) { pick = a; break; } }
                String alt = pick.optString("alt_baro", "ground");
                org.json.JSONArray r = new org.json.JSONArray();
                r.put("ground".equals(alt) ? 0 : 1); r.put(alt); r.put(pick.optDouble("gs", 0));
                out.put(cs, r);
              }
              break;
            } catch (Exception e) { }
          }
          try { Thread.sleep(400); } catch (Exception e) { }
        }
        try { if (okc == 0) out.put("_err", 1); } catch (Exception e) { }
        final String js = "window.__live(" + out.toString() + ")";
        MainActivity.this.runOnUiThread(new Runnable() { public void run() { web.evaluateJavascript(js, null); } });
      } }).start();
    }
  }
  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().setStatusBarColor(Color.parseColor("#0A1A2C"));
    getWindow().setNavigationBarColor(Color.parseColor("#0A1A2C"));
    web = new WebView(this);
    web.setBackgroundColor(Color.parseColor("#0A1A2C"));
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setSupportMultipleWindows(true);
    s.setJavaScriptCanOpenWindowsAutomatically(true);
    web.addJavascriptInterface(new Bridge(), "AndroidLive");
    web.setWebViewClient(new WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
        Uri u = r.getUrl();
        if ("file".equals(u.getScheme())) return false;
        ext(u);
        return true;
      }
    });
    web.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onCreateWindow(WebView v, boolean d, boolean g, Message m) {
        WebView t = new WebView(MainActivity.this);
        t.setWebViewClient(new WebViewClient() {
          @Override public boolean shouldOverrideUrlLoading(WebView x, WebResourceRequest r) { ext(r.getUrl()); return true; }
          @Override public void onPageStarted(WebView x, String url, Bitmap f) { if (url != null && !url.startsWith("about:")) { ext(Uri.parse(url)); x.stopLoading(); } }
        });
        ((WebView.WebViewTransport) m.obj).setWebView(t);
        m.sendToTarget();
        return true;
      }
    });
    setContentView(web);
    web.loadUrl("file:///android_asset/index.html");
  }
  @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
