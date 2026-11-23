package app.chessquest;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import org.json.JSONObject;

public class MainActivity extends Activity {
    WebView wv;
    Process p;
    BufferedWriter out;

    class Bridge {
        @JavascriptInterface
        public boolean start() {
            try {
                if (p != null) return true;
                String path = getApplicationInfo().nativeLibraryDir + "/libstockfish.so";
                p = new ProcessBuilder(path).redirectErrorStream(true).start();
                out = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
                new Thread(() -> {
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                        String l;
                        while ((l = r.readLine()) != null) {
                            final String x = l;
                            runOnUiThread(() -> wv.evaluateJavascript("window.onSF(" + JSONObject.quote(x) + ")", null));
                        }
                    } catch (Exception e) { }
                }).start();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public void send(String c) {
            try { out.write(c + "\n"); out.flush(); } catch (Exception e) { }
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        wv = new WebView(this);
        setContentView(wv);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        wv.addJavascriptInterface(new Bridge(), "AndroidSF");
        wv.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onDestroy() {
        if (p != null) p.destroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (wv.canGoBack()) wv.goBack(); else super.onBackPressed();
    }
}
