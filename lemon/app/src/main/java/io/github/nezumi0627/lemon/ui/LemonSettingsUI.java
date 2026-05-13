package io.github.nezumi0627.lemon.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import io.github.nezumi0627.lemon.utils.ChangelogManager;
import io.github.nezumi0627.lemon.utils.IntentUtils;
import io.github.nezumi0627.lemon.utils.LemonSettings;
import io.github.nezumi0627.lemon.utils.ModuleAssetReader;
import io.github.nezumi0627.lemon.utils.ProfileResolver;
import io.github.nezumi0627.lemon.utils.ThemeDownloader;
import io.github.nezumi0627.lemon.utils.ThemeManager;
import io.github.nezumi0627.lemon.utils.ThemeStockManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.nezumi0627.lemon.constants.LemonConstants;

/**
 * LEMONモジュールの設定や情報を表示する独自ダイアログUI。
 * アニメーション、永続化、プロフィール表示、サブ画面遷移をサポート。
 */
public class LemonSettingsUI {
    private static final AtomicBoolean themeOperationInProgress = new AtomicBoolean(false);

    public static void show(Context context) {
        show(context, false, null);
    }

    public static void show(Context context, final boolean isBottomUp) {
        show(context, isBottomUp, null);
    }

    public static void show(Context context, final boolean isBottomUp, final String currentChatId) {
        // ダイアログの親レイアウト
        final FrameLayout rootFrame = new FrameLayout(context);
        rootFrame.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.WHITE);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootFrame.addView(rootLayout);

        // フルスクリーンダイアログ
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        final AlertDialog dialog = builder.create();

        // 戻るアクション
        final Runnable backAction = () -> {
            if (isBottomUp) {
                rootLayout.animate()
                        .translationY(rootLayout.getHeight())
                        .setDuration(300)
                        .withEndAction(dialog::dismiss)
                        .start();
            } else {
                rootLayout.animate()
                        .translationX(rootLayout.getWidth())
                        .setDuration(250)
                        .withEndAction(dialog::dismiss)
                        .start();
            }
        };

        // ネイティブ風ヘッダー
        View header = LemonUIBuilder.createNativeHeader(context, "LEMON🍋", v -> backAction.run());
        rootLayout.addView(header);

        // タブコンテナ
        LinearLayout tabContainer = new LinearLayout(context);
        tabContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rootLayout.addView(tabContainer);

        // コンテンツ領域
        final FrameLayout contentContainer = new FrameLayout(context);
        contentContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.addView(contentContainer);

        final View infoView = createInfoView(context, currentChatId);
        final View settingsView = createSettingsView(context);
        final View themeView = createThemeView(context);

        contentContainer.addView(infoView);
        contentContainer.addView(settingsView);
        contentContainer.addView(themeView);

        // 初期表示設定
        infoView.setVisibility(View.VISIBLE);
        settingsView.setVisibility(View.GONE);
        themeView.setVisibility(View.GONE);

        final TextView tabInfo = createTabButton(context, "Info");
        final TextView tabSettings = createTabButton(context, "Settings");
        final TextView tabTheme = createTabButton(context, "Theme");

        tabContainer.addView(tabInfo);
        tabContainer.addView(tabSettings);
        tabContainer.addView(tabTheme);

        // タブ切り替えロジック
        tabInfo.setOnClickListener(v -> {
            infoView.setVisibility(View.VISIBLE);
            settingsView.setVisibility(View.GONE);
            themeView.setVisibility(View.GONE);
            updateTabStyle(tabInfo, true);
            updateTabStyle(tabSettings, false);
            updateTabStyle(tabTheme, false);
        });

        tabSettings.setOnClickListener(v -> {
            settingsView.setVisibility(View.VISIBLE);
            infoView.setVisibility(View.GONE);
            themeView.setVisibility(View.GONE);
            updateTabStyle(tabSettings, true);
            updateTabStyle(tabInfo, false);
            updateTabStyle(tabTheme, false);
        });

        tabTheme.setOnClickListener(v -> {
            themeView.setVisibility(View.VISIBLE);
            infoView.setVisibility(View.GONE);
            settingsView.setVisibility(View.GONE);
            updateTabStyle(tabTheme, true);
            updateTabStyle(tabInfo, false);
            updateTabStyle(tabSettings, false);
        });

        // 初期スタイル適用
        updateTabStyle(tabInfo, true);
        updateTabStyle(tabSettings, false);
        updateTabStyle(tabTheme, false);

        dialog.setView(rootFrame);
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                backAction.run();
                return true;
            }
            return false;
        });

        dialog.show();

        // アニメーション開始
        rootLayout.post(() -> {
            if (isBottomUp) {
                rootLayout.setTranslationY(rootLayout.getHeight());
                rootLayout.animate().translationY(0).setDuration(300).start();
            } else {
                rootLayout.setTranslationX(rootLayout.getWidth());
                rootLayout.animate().translationX(0).setDuration(250).start();
            }
        });
    }

    private static TextView createTabButton(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dpToPx(context, 12), 0, dpToPx(context, 12));
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private static void updateTabStyle(TextView tv, boolean active) {
        if (active) {
            tv.setTextColor(Color.parseColor("#333333"));
            tv.setBackgroundColor(Color.parseColor("#FDFDFD"));
        } else {
            tv.setTextColor(Color.parseColor("#AAAAAA"));
            tv.setBackgroundColor(Color.WHITE);
        }
    }

    private static View createSettingsView(Context context) {
        ScrollView scrollView = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(Color.parseColor("#F4F4F4"));

        container.addView(LemonUIBuilder.createSectionHeader(context, "GENERAL"));
        container.addView(LemonUIBuilder.createSwitchItem(context, "広告ブロック", "トーク一覧やニュースの広告を非表示にします", LemonConstants.KEY_AD_BLOCK, true));
        container.addView(LemonUIBuilder.createDivider(context));
        container.addView(LemonUIBuilder.createSwitchItem(context, "既読無効化", "チャット個別設定がONのトークで既読送信を抑止します", LemonConstants.KEY_READ_RECEIPT_BLOCK_ENABLED, false));
        container.addView(LemonUIBuilder.createDivider(context));
        container.addView(LemonUIBuilder.createSwitchItem(context, "送信時に既読を付ける", "既読無効中のトークで、送信直後に短時間だけ既読をサーバーへ送ります", LemonConstants.KEY_READ_RECEIPT_SEND_ON_SEND, false));
        container.addView(LemonUIBuilder.createDivider(context));
        container.addView(LemonUIBuilder.createSwitchItem(context, "トーク上部の既読ボタン", "トーク画面ヘッダーに目アイコンの切替ボタンを表示します", LemonConstants.KEY_READ_RECEIPT_HEADER_BUTTON_ENABLED, true));

        container.addView(LemonUIBuilder.createSectionHeader(context, "EXPERIMENTAL"));
        // AIアシスタント設定は常にONに固定されたため削除

        container.addView(LemonUIBuilder.createSectionHeader(context, "SYSTEM"));
        container.addView(LemonUIBuilder.createNavigationItem(context, "設定ファイルの保存場所", "内部ストレージ上の JSON", v -> showSettingsStorageDialog(context)));
        container.addView(LemonUIBuilder.createDivider(context));
        container.addView(LemonUIBuilder.createSwitchItem(context, "起動トースト", "LINE起動時にLEMONの読み込み通知を表示します", LemonConstants.KEY_STARTUP_TOAST_ENABLED, false));
        container.addView(LemonUIBuilder.createDivider(context));
        container.addView(LemonUIBuilder.createNavigationItem(context, "ログ設定", "詳細", v -> showLogSettings(context)));

        scrollView.addView(container);
        return scrollView;
    }

    private static View createThemeView(Context context) {
        ScrollView scrollView = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(Color.parseColor("#F4F4F4"));

        container.addView(LemonUIBuilder.createSectionHeader(context, "THEME MANAGER"));
        container.addView(LemonUIBuilder.createNavigationItem(context, "テーマを追加", "URL / Theme ID", v -> showThemeAddDialog(context)));
        container.addView(LemonUIBuilder.createDivider(context));
        container.addView(LemonUIBuilder.createNavigationItem(context, "バックアップを作成", "安全に保存", v -> createManualBackup(context)));
        container.addView(LemonUIBuilder.createDivider(context));
        container.addView(LemonUIBuilder.createNavigationItem(context, "バックアップから復元", "一覧から選択", v -> showVersionedBackupRestoreDialog(context)));

        String activeThemeId = ThemeStockManager.getActiveStockId(context);
        container.addView(LemonUIBuilder.createSectionHeader(context, "ACTIVE"));
        container.addView(LemonUIBuilder.createNavigationItem(context, "使用中のストック", activeThemeId.isEmpty() ? "未選択" : activeThemeId, null));

        List<ThemeStockManager.ThemeStock> stocks = ThemeStockManager.getStocks(context);
        container.addView(LemonUIBuilder.createSectionHeader(context, "STOCKS (" + stocks.size() + ")"));
        if (stocks.isEmpty()) {
            container.addView(LemonUIBuilder.createNavigationItem(context, "ストックなし", "URLから追加してください", null));
        } else {
            for (ThemeStockManager.ThemeStock stock : stocks) {
                container.addView(ThemeStockRowFactory.create(context, stock, v -> showThemeStockActions(context, stock)));
                container.addView(LemonUIBuilder.createDivider(context));
            }
        }

        scrollView.addView(container);
        return scrollView;
    }

    private static View createInfoView(Context context, String currentChatId) {
        ScrollView scrollView = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(Color.parseColor("#F4F4F4"));

        // プロフィールセクション
        ProfileResolver.ProfileData profile = ProfileResolver.resolve(context);
        container.addView(LemonUIBuilder.createProfileSection(
                context,
                profile.name,
                profile.iconUrl,
                profile.mid,
                currentChatId
        ));

        container.addView(LemonUIBuilder.createSectionHeader(context, "THEME INFO"));
        String themeId = getCurrentThemeId(context);
        container.addView(LemonUIBuilder.createNavigationItem(context, "現在のテーマID", themeId, v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("ThemeID", themeId);
            clipboard.setPrimaryClip(clip);
            android.widget.Toast.makeText(context, "Theme ID copied!", android.widget.Toast.LENGTH_SHORT).show();
        }));

        container.addView(LemonUIBuilder.createSectionHeader(context, "ABOUT LEMON"));
        container.addView(LemonUIBuilder.createNavigationItem(context, "バージョン", "v" + LemonConstants.MODULE_VERSION, v -> showVersionDetails(context)));
        container.addView(LemonUIBuilder.createDivider(context));
        container.addView(LemonUIBuilder.createNavigationItem(context, "詳細情報", null, v -> showInfoDetails(context)));

        container.addView(LemonUIBuilder.createSectionHeader(context, "DEVELOPER"));
        container.addView(LemonUIBuilder.createGitHubLink(context, "nezumi0627"));

        scrollView.addView(container);
        return scrollView;
    }

    private static void showLogSettings(Context context) {
        showSubScreen(context, "ログ設定", container -> {
            container.addView(LemonUIBuilder.createSectionHeader(context, "DEBUG LOG"));
            container.addView(LemonUIBuilder.createSwitchItem(context, "ログ出力を有効化", "Logcatにデバッグ情報を出力します", LemonConstants.KEY_LOG_ENABLED, false));
        });
    }

    private static void showSettingsStorageDialog(Context context) {
        File def = new File(Environment.getExternalStorageDirectory(),
                LemonConstants.DEFAULT_SETTINGS_SUBDIR + "/" + LemonConstants.DEFAULT_SETTINGS_FILENAME);
        String defPath = def.getAbsolutePath();
        String curOverride = LemonSettings.getSettingsJsonPathOverride(context);
        String initial = !curOverride.isEmpty() ? curOverride : defPath;

        EditText et = new EditText(context);
        et.setText(initial);
        et.setHint(defPath);
        int pad = dpToPx(context, 12);
        et.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(context)
                .setTitle("設定 JSON のパス")
                .setMessage("デフォルトは外部ストレージ上の LEMON フォルダです。空欄で保存するとデフォルトに戻します。")
                .setView(et)
                .setPositiveButton("保存", (d, w) -> {
                    String t = et.getText() == null ? "" : et.getText().toString().trim();
                    if (t.isEmpty() || t.equals(defPath)) {
                        LemonSettings.setSettingsJsonPathOverrideFromHostApp(context, "");
                    } else {
                        LemonSettings.setSettingsJsonPathOverrideFromHostApp(context, t);
                    }
                    Toast.makeText(context, "保存しました。LINE を再起動してください。", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private static void showInfoDetails(Context context) {
        showSubScreen(context, "詳細情報", container -> {
            container.addView(LemonUIBuilder.createSectionHeader(context, "CONTRIBUTORS"));
            container.addView(LemonUIBuilder.createNavigationItem(context, "nezumi0627", "Lead Developer", v -> {
                openUrl(context, LemonConstants.GITHUB_URL + "/nezumi0627");
            }));
            container.addView(LemonUIBuilder.createDivider(context));
            container.addView(LemonUIBuilder.createSectionHeader(context, "LICENSE"));
            container.addView(LemonUIBuilder.createNavigationItem(context, "Open Source Licenses", "表示", v -> showLicenseText(context)));
        });
    }

    private static void showVersionDetails(Context context) {
        showSubScreen(context, "バージョン情報", container -> {
            container.addView(LemonUIBuilder.createSectionHeader(context, "CURRENT"));
            container.addView(LemonUIBuilder.createNavigationItem(context, "LEMON", "v" + LemonConstants.MODULE_VERSION, null));
            container.addView(LemonUIBuilder.createDivider(context));

            container.addView(LemonUIBuilder.createSectionHeader(context, "AUTO CHANGELOG"));
            TextView history = new TextView(context);
            history.setText(ChangelogManager.buildHistoryText(context));
            history.setTextColor(Color.parseColor("#333333"));
            history.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            history.setPadding(dpToPx(context, 16), dpToPx(context, 10), dpToPx(context, 16), dpToPx(context, 16));
            history.setBackgroundColor(Color.WHITE);
            container.addView(history);
        });
    }

    private static void showLicenseText(Context context) {
        showSubScreen(context, "LICENSE", container -> {
            TextView tv = new TextView(context);
            tv.setText(readAssetText(context, "LICENSE.txt", "LICENSE を読み込めませんでした。"));
            tv.setTextColor(Color.parseColor("#333333"));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tv.setPadding(dpToPx(context, 16), dpToPx(context, 12), dpToPx(context, 16), dpToPx(context, 16));
            tv.setBackgroundColor(Color.WHITE);
            container.addView(tv);
        });
    }

    private static String readAssetText(Context context, String fileName, String fallback) {
        try {
            return ModuleAssetReader.readText(context, fileName);
        } catch (Exception primary) {
            try (java.io.InputStream in = context.getAssets().open(fileName);
                 java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                return out.toString("UTF-8");
            } catch (Exception secondary) {
                return fallback;
            }
        }
    }

    private static void showThemeApplyDialog(Context context) {
        final android.widget.EditText input = new android.widget.EditText(context);
        input.setHint("Theme ID (e.g. ec4a14ea-...)");

        new AlertDialog.Builder(context)
            .setTitle("テーマの適用")
            .setMessage("テーマIDを入力すると、自動的にダウンロードして適用します。\n変更前にバックアップが作成されます。")
            .setView(input)
            .setPositiveButton("適用", (dialog, which) -> {
                if (!themeOperationInProgress.compareAndSet(false, true)) {
                    android.widget.Toast.makeText(context, "テーマ処理が進行中です", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                String themeId = input.getText().toString().trim();
                if (themeId.isEmpty()) {
                    themeOperationInProgress.set(false);
                    android.widget.Toast.makeText(context, "テーマIDを入力してください", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                showProgressDialog(context, "テーマを適用中...", () -> {
                    ThemeManager.applyTheme(context, themeId, new ThemeManager.ThemeCallback() {
                        @Override
                        public void onSuccess() {
                            finishThemeOperation(() -> showRestartRequiredDialog(context, "テーマの適用が完了しました"));
                        }

                        @Override
                        public void onError(String error) {
                            finishThemeOperation(() -> android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show());
                        }

                        @Override
                        public void onProgress(String message) {
                            updateProgressDialog(message);
                        }
                    });
                });
            })
            .setNegativeButton("キャンセル", null)
            .show();
    }

    private static void showThemeAddDialog(Context context) {
        final android.widget.EditText input = new android.widget.EditText(context);
        input.setHint("Theme URL または Theme ID");
        new AlertDialog.Builder(context)
                .setTitle("テーマを追加")
                .setMessage("LINE STOREのURLを貼るか、Theme IDを入力してください。")
                .setView(input)
                .setPositiveButton("追加", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        showToast(context, "入力してください");
                        return;
                    }
                    showProgressDialog(context, "テーマ情報を取得中...", () -> new Thread(() -> {
                        try {
                            ThemeStockManager.ThemeStock stock = ThemeStockManager.fetchStockFromInput(value);
                            ThemeStockManager.upsertStock(context, stock);
                            runOnMain(() -> {
                                hideProgressDialog();
                                showToast(context, "ストックに追加しました: " + stock.name);
                                reopenSettings(context);
                            });
                        } catch (Exception e) {
                            runOnMain(() -> {
                                hideProgressDialog();
                                showToastLong(context, e.getMessage());
                            });
                        }
                    }, "LemonThemeAdd").start());
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private static void showThemeStockActions(Context context, ThemeStockManager.ThemeStock stock) {
        String[] items = new String[] {"適用", "更新して適用", "情報更新", "ストアで開く", "削除"};
        new AlertDialog.Builder(context)
                .setTitle(stock.name)
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            applyThemeStock(context, stock.themeId);
                            break;
                        case 1:
                            updateAndApplyThemeStock(context, stock.themeId);
                            break;
                        case 2:
                            refreshThemeStock(context, stock.themeId);
                            break;
                        case 3:
                            if (stock.storeUrl != null && !stock.storeUrl.isEmpty()) {
                                openUrl(context, stock.storeUrl);
                            } else {
                                openUrl(context, "https://store.line.me/themeshop/product/" + stock.themeId + "/ja");
                            }
                            break;
                        case 4:
                            ThemeStockManager.deleteStock(context, stock.themeId);
                            showToast(context, "削除しました");
                            reopenSettings(context);
                            break;
                        default:
                            break;
                    }
                })
                .setNegativeButton("閉じる", null)
                .show();
    }

    private static void applyThemeStock(Context context, String themeId) {
        if (!themeOperationInProgress.compareAndSet(false, true)) {
            showToast(context, "テーマ処理が進行中です");
            return;
        }
        showProgressDialog(context, "テーマを適用中...", () -> ThemeManager.applyTheme(context, themeId, new ThemeManager.ThemeCallback() {
            @Override
            public void onSuccess() {
                ThemeStockManager.setActiveStockId(context, themeId);
                finishThemeOperation(() -> showRestartRequiredDialog(context, "テーマの適用が完了しました"));
            }

            @Override
            public void onError(String error) {
                finishThemeOperation(() -> showToastLong(context, error));
            }

            @Override
            public void onProgress(String message) {
                updateProgressDialog(message);
            }
        }));
    }

    private static void updateAndApplyThemeStock(Context context, String themeId) {
        if (!themeOperationInProgress.compareAndSet(false, true)) {
            showToast(context, "テーマ処理が進行中です");
            return;
        }
        showProgressDialog(context, "バージョン確認中...", () -> new Thread(() -> {
            try {
                ThemeStockManager.ThemeStock stock = ThemeStockManager.getStock(context, themeId);
                if (stock == null) throw new Exception("ストックが見つかりません");
                int latest = ThemeDownloader.getLatestVersion(themeId);
                if (latest != stock.currentVersion) {
                    stock.previousVersion = stock.currentVersion;
                    stock.currentVersion = latest;
                    stock.updatedAt = System.currentTimeMillis();
                    if (stock.thumbnailUrl == null || stock.thumbnailUrl.isEmpty()) {
                        stock.thumbnailUrl = ThemeDownloader.buildStoreIconUrl(themeId, latest);
                    }
                    ThemeStockManager.upsertStock(context, stock);
                }
                runOnMain(() -> updateProgressDialog("テーマを適用中..."));
                ThemeManager.applyTheme(context, themeId, new ThemeManager.ThemeCallback() {
                    @Override
                    public void onSuccess() {
                        ThemeStockManager.setActiveStockId(context, themeId);
                        finishThemeOperation(() -> showRestartRequiredDialog(context, "更新と適用が完了しました"));
                    }

                    @Override
                    public void onError(String error) {
                        finishThemeOperation(() -> showToastLong(context, error));
                    }

                    @Override
                    public void onProgress(String message) {
                        updateProgressDialog(message);
                    }
                });
            } catch (Exception e) {
                finishThemeOperation(() -> showToastLong(context, e.getMessage()));
            }
        }, "LemonThemeUpdateApply").start());
    }

    private static void refreshThemeStock(Context context, String themeId) {
        showProgressDialog(context, "テーマ情報を更新中...", () -> new Thread(() -> {
            try {
                ThemeStockManager.ThemeStock stock = ThemeStockManager.getStock(context, themeId);
                if (stock == null) throw new Exception("ストックが見つかりません");
                int latest = ThemeDownloader.getLatestVersion(themeId);
                if (latest != stock.currentVersion) {
                    stock.previousVersion = stock.currentVersion;
                    stock.currentVersion = latest;
                    stock.updatedAt = System.currentTimeMillis();
                    ThemeStockManager.upsertStock(context, stock);
                }
                runOnMain(() -> {
                    hideProgressDialog();
                    showToast(context, "更新しました (V" + latest + ")");
                    reopenSettings(context);
                });
            } catch (Exception e) {
                runOnMain(() -> {
                    hideProgressDialog();
                    showToastLong(context, e.getMessage());
                });
            }
        }, "LemonThemeRefresh").start());
    }

    private static void createManualBackup(Context context) {
        new Thread(() -> {
            try {
                String name = ThemeManager.createVersionedBackup(context, "manual");
                runOnMain(() -> showToastLong(context, "バックアップを作成: " + name));
            } catch (Exception e) {
                runOnMain(() -> showToastLong(context, "バックアップ作成失敗: " + e.getMessage()));
            }
        }, "LemonThemeManualBackup").start();
    }

    private static void showVersionedBackupRestoreDialog(Context context) {
        List<String> backups = ThemeManager.listVersionedBackups(context);
        if (backups.isEmpty()) {
            showThemeRestoreDialog(context);
            return;
        }
        String[] items = backups.toArray(new String[0]);
        new AlertDialog.Builder(context)
                .setTitle("復元するバックアップを選択")
                .setItems(items, (dialog, which) -> {
                    if (!themeOperationInProgress.compareAndSet(false, true)) {
                        showToast(context, "テーマ処理が進行中です");
                        return;
                    }
                    String name = items[which];
                    showProgressDialog(context, "バックアップ復元中...", () -> ThemeManager.restoreVersionedBackup(context, name, new ThemeManager.ThemeCallback() {
                        @Override
                        public void onSuccess() {
                            finishThemeOperation(() -> showRestartRequiredDialog(context, "バックアップ復元が完了しました"));
                        }

                        @Override
                        public void onError(String error) {
                            finishThemeOperation(() -> showToastLong(context, error));
                        }

                        @Override
                        public void onProgress(String message) {
                            updateProgressDialog(message);
                        }
                    }));
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private static void showThemeRestoreDialog(Context context) {
        if (!ThemeManager.backupExists(context)) {
            new AlertDialog.Builder(context)
                .setTitle("テーマの復元")
                .setMessage("バックアップが見つかりません")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        new AlertDialog.Builder(context)
            .setTitle("テーマの復元")
            .setMessage("バックアップからテーマを復元しますか？")
            .setPositiveButton("復元", (dialog, which) -> {
                if (!themeOperationInProgress.compareAndSet(false, true)) {
                    showToast(context, "テーマ処理が進行中です");
                    return;
                }

                showProgressDialog(context, "テーマを復元中...", () -> {
                    ThemeManager.restoreBackup(context, new ThemeManager.ThemeCallback() {
                        @Override
                        public void onSuccess() {
                            finishThemeOperation(() -> showRestartRequiredDialog(context, "テーマの復元が完了しました"));
                        }

                        @Override
                        public void onError(String error) {
                            finishThemeOperation(() -> showToastLong(context, error));
                        }

                        @Override
                        public void onProgress(String message) {
                            updateProgressDialog(message);
                        }
                    });
                });
            })
            .setNegativeButton("キャンセル", null)
            .show();
    }

    private static void showRestartRequiredDialog(Context context, String title) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage("LINEを完全に終了してから再起動すると反映されます。自動再起動はループ防止のため行いません。")
                .setPositiveButton("OK", null)
                .show();
    }

    private static void showSubScreen(Context context, String title, SubScreenBuilder builder) {
        final LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#F4F4F4"));

        AlertDialog.Builder ab = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        final AlertDialog dialog = ab.create();

        View header = LemonUIBuilder.createNativeHeader(context, title, v -> {
            rootLayout.animate().translationX(rootLayout.getWidth()).setDuration(250).withEndAction(dialog::dismiss).start();
        });
        rootLayout.addView(header);

        ScrollView scrollView = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        builder.build(container);
        scrollView.addView(container);
        rootLayout.addView(scrollView);

        dialog.setView(rootLayout);
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                rootLayout.animate().translationX(rootLayout.getWidth()).setDuration(250).withEndAction(dialog::dismiss).start();
                return true;
            }
            return false;
        });

        dialog.show();
        rootLayout.post(() -> {
            rootLayout.setTranslationX(rootLayout.getWidth());
            rootLayout.animate().translationX(0).setDuration(250).start();
        });
    }

    private interface SubScreenBuilder {
        void build(LinearLayout container);
    }

    private static String getCurrentThemeId(Context context) {
        try {
            if (LemonSettings.getBoolean(context, LemonConstants.KEY_THEME_FORCE_ENABLED, false)) {
                return LemonSettings.getString(context, LemonConstants.KEY_THEME_FORCE_ID, LemonConstants.THEME_ID_DEFAULT);
            }
            android.content.SharedPreferences prefs = context.getSharedPreferences("ThemeManager", Context.MODE_PRIVATE);
            return prefs.getString("ThemePackageName", LemonConstants.THEME_ID_DEFAULT);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private static AlertDialog progressDialog;
    private static TextView progressMessage;

    private static void showProgressDialog(Context context, String title, Runnable onShow) {
        if (progressDialog != null && progressDialog.isShowing()) {
            updateProgressDialog(title);
            return;
        }

        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(dpToPx(context, 20), dpToPx(context, 20), dpToPx(context, 20), dpToPx(context, 20));

        progressMessage = new TextView(context);
        progressMessage.setText(title);
        progressMessage.setTextSize(16);
        progressMessage.setPadding(0, 0, 0, dpToPx(context, 16));

        rootLayout.addView(progressMessage);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(rootLayout);
        builder.setCancelable(false);

        progressDialog = builder.create();
        progressDialog.show();

        if (onShow != null) {
            onShow.run();
        }
    }

    private static void updateProgressDialog(String message) {
        if (progressDialog != null && progressMessage != null) {
            runOnMain(() -> {
                if (progressMessage != null) {
                    progressMessage.setText(message);
                }
            });
        }
    }

    private static void hideProgressDialog() {
        if (progressDialog != null) {
            try {
                progressDialog.dismiss();
            } catch (Throwable ignored) {
            }
            progressDialog = null;
            progressMessage = null;
        }
    }

    private static void finishThemeOperation(Runnable afterDismiss) {
        runOnMain(() -> {
            themeOperationInProgress.set(false);
            hideProgressDialog();
            afterDismiss.run();
        });
    }

    private static void runOnMain(Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
            return;
        }
        new Handler(Looper.getMainLooper()).post(action);
    }

    private static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    private static void openUrl(Context context, String url) {
        if (!IntentUtils.openUrl(context, url)) {
            showToastLong(context, "URLを開けませんでした");
        }
    }

    private static void reopenSettings(Context context) {
        show(context, false, null);
    }

    private static void showToast(Context context, String message) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    private static void showToastLong(Context context, String message) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show();
    }
}
