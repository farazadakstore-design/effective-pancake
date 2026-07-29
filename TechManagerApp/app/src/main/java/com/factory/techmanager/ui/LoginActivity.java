package com.factory.techmanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.factory.techmanager.R;
import com.factory.techmanager.data.Department;
import com.factory.techmanager.data.Repository;
import com.factory.techmanager.data.Seed;
import com.factory.techmanager.data.Session;
import com.factory.techmanager.data.SettingsModel;
import com.factory.techmanager.util.AppState;
import com.factory.techmanager.util.Lang;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private Repository repo;
    private GridLayout deptGrid;
    private EditText pwInput;
    private TextView tvErr, tvTitle, tvSub;
    private android.widget.Button btnLangToggle, btnLogin;
    private final List<LinearLayout> cardViews = new ArrayList<>();
    private final List<String> cardIds = new ArrayList<>(); // "admin" or dept id

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        repo = new Repository(this);
        Lang.cur = repo.getLang();
        AppState.resetForLogout();

        deptGrid = findViewById(R.id.deptGrid);
        pwInput = findViewById(R.id.pwInput);
        tvErr = findViewById(R.id.tvLoginErr);
        tvTitle = findViewById(R.id.tvLoginTitle);
        tvSub = findViewById(R.id.tvLoginSub);
        btnLangToggle = findViewById(R.id.btnLangToggle);
        btnLogin = findViewById(R.id.btnLogin);

        btnLangToggle.setOnClickListener(v -> {
            Lang.toggle();
            repo.setLang(Lang.cur);
            applyLang();
            buildGrid();
        });
        btnLogin.setOnClickListener(v -> doLogin());
        pwInput.setOnEditorActionListener((v, actionId, event) -> {
            doLogin();
            return true;
        });

        applyLang();
        buildGrid();
        applyLayoutDirection();
    }

    private void applyLayoutDirection() {
        int dir = Lang.isArabic() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR;
        findViewById(android.R.id.content).setLayoutDirection(dir);
    }

    private void applyLang() {
        tvTitle.setText(Lang.t("اختر القسم", "Select Department"));
        tvSub.setText(Lang.t("ثم أدخل كلمة المرور", "Then enter your password"));
        btnLogin.setText(Lang.t("دخول", "Login"));
        applyLayoutDirection();
    }

    private void buildGrid() {
        deptGrid.removeAllViews();
        cardViews.clear();
        cardIds.clear();

        addCard("admin", "🔑", Lang.t("أدمن", "Admin"));
        for (Department d : Seed.DEPTS) {
            addCard(d.id, d.icon, d.label(Lang.isArabic()));
        }
        refreshSelection();
    }

    private void addCard(String id, String icon, String name) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        int pad = dp(12);
        card.setPadding(pad, dp(14), pad, dp(14));
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.columnSpec = GridLayout.spec(cardIds.size() % 2, 1f);
        lp.rowSpec = GridLayout.spec(cardIds.size() / 2);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        card.setLayoutParams(lp);

        TextView tvIcon = new TextView(this);
        tvIcon.setText(icon);
        tvIcon.setTextSize(24);
        tvIcon.setGravity(Gravity.CENTER);
        card.addView(tvIcon);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(13);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setTextColor(getColor(R.color.primary_dark));
        tvName.setGravity(Gravity.CENTER);
        tvName.setPadding(0, dp(4), 0, 0);
        card.addView(tvName);

        card.setOnClickListener(v -> {
            AppState.selectedLoginDept = id;
            refreshSelection();
        });

        deptGrid.addView(card);
        cardViews.add(card);
        cardIds.add(id);
    }

    private void refreshSelection() {
        for (int i = 0; i < cardViews.size(); i++) {
            boolean sel = cardIds.get(i).equals(AppState.selectedLoginDept);
            cardViews.get(i).setBackgroundResource(sel ? R.drawable.bg_login_card_selected : R.drawable.bg_login_card_default);
        }
    }

    private void doLogin() {
        String pw = pwInput.getText().toString().trim();
        if (AppState.selectedLoginDept == null) {
            tvErr.setText(Lang.t("اختر قسماً أولاً", "Select a department first"));
            return;
        }
        SettingsModel settings = repo.getSettings();
        if ("admin".equals(AppState.selectedLoginDept)) {
            if (!pw.equals(settings.adminPw)) {
                tvErr.setText(Lang.t("كلمة مرور خاطئة", "Wrong password"));
                return;
            }
            AppState.session = new Session("admin", "admin");
        } else {
            String correct = settings.pwFor(AppState.selectedLoginDept);
            if (!pw.equals(correct)) {
                tvErr.setText(Lang.t("كلمة مرور خاطئة", "Wrong password"));
                return;
            }
            AppState.session = new Session(AppState.selectedLoginDept, "dept");
        }
        tvErr.setText("");
        pwInput.setText("");
        startActivity(new Intent(this, MainActivity.class));
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }
}
