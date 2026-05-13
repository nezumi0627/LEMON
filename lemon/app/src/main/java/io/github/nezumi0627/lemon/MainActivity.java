package io.github.nezumi0627.lemon;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import io.github.nezumi0627.lemon.constants.LemonConstants;

/**
 * LEMON🍋 アプリ本体の設定・情報画面。
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // シンプルなレイアウトを動的に生成、または R.layout.activity_main を使用
        // ここでは将来の拡張を見据えて、基本的な情報を表示する
        setContentView(R.layout.activity_main);

        TextView textView = findViewById(R.id.textView);
        if (textView != null) {
            StringBuilder info = new StringBuilder();
            info.append(LemonConstants.MODULE_NAME).append(" 🍋\n");
            info.append("Version: ").append(LemonConstants.MODULE_VERSION).append("\n");
            info.append("Author: ").append(LemonConstants.MODULE_AUTHOR).append("\n\n");
            info.append("このモジュールは LINE アプリの UI をカスタマイズします。\n");
            info.append("LSPosed 等のマネージャーで有効化し、LINE を再起動してください。\n\n");
            info.append("注意: 教育目的の研究用プロジェクトです。");
            
            textView.setText(info.toString());
        }
    }
}
