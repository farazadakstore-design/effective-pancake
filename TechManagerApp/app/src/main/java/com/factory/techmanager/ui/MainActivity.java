package com.factory.techmanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.factory.techmanager.R;
import com.factory.techmanager.data.Department;
import com.factory.techmanager.data.Repository;
import com.factory.techmanager.data.Seed;
import com.factory.techmanager.ui.fragments.EvaluationFragment;
import com.factory.techmanager.ui.fragments.HomeFragment;
import com.factory.techmanager.ui.fragments.ReportFragment;
import com.factory.techmanager.ui.fragments.SettingsFragment;
import com.factory.techmanager.util.AppState;
import com.factory.techmanager.util.Lang;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private Repository repo;
    private BottomNavigationView bottomNav;
    private android.widget.TextView tvHdrDept, tvHdrSub;
    private android.widget.Button btnHdrLang, btnHdrLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AppState.session == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        repo = new Repository(this);
        Lang.cur = repo.getLang();

        tvHdrDept = findViewById(R.id.tvHdrDept);
        tvHdrSub = findViewById(R.id.tvHdrSub);
        btnHdrLang = findViewById(R.id.btnHdrLang);
        btnHdrLogout = findViewById(R.id.btnHdrLogout);
        bottomNav = findViewById(R.id.bottomNav);

        btnHdrLang.setOnClickListener(v -> {
            Lang.toggle();
            repo.setLang(Lang.cur);
            applyLayoutDirection();
            refreshHeader();
            rebuildNavLabels();
            reloadCurrentFragment();
        });
        btnHdrLogout.setOnClickListener(v -> {
            AppState.resetForLogout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        bottomNav.getMenu().findItem(R.id.nav_settings).setVisible(AppState.session.isAdmin());
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { showFragment(new HomeFragment()); return true; }
            if (id == R.id.nav_report) { showFragment(new ReportFragment()); return true; }
            if (id == R.id.nav_eval) { showFragment(new EvaluationFragment()); return true; }
            if (id == R.id.nav_settings) { showFragment(new SettingsFragment()); return true; }
            return false;
        });

        applyLayoutDirection();
        refreshHeader();
        rebuildNavLabels();
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private void applyLayoutDirection() {
        int dir = Lang.isArabic() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR;
        findViewById(android.R.id.content).setLayoutDirection(dir);
    }

    private void rebuildNavLabels() {
        bottomNav.getMenu().findItem(R.id.nav_home).setTitle(Lang.t("الرئيسية", "Home"));
        bottomNav.getMenu().findItem(R.id.nav_report).setTitle(Lang.t("التقرير", "Report"));
        bottomNav.getMenu().findItem(R.id.nav_eval).setTitle(Lang.t("التقييم", "Evaluation"));
        bottomNav.getMenu().findItem(R.id.nav_settings).setTitle(Lang.t("الضبط", "Settings"));
        btnHdrLogout.setText(Lang.t("خروج", "Logout"));
    }

    private void refreshHeader() {
        boolean admin = AppState.session.isAdmin();
        if (admin) {
            tvHdrDept.setText(Lang.t("لوحة الإدارة", "Admin Panel"));
            tvHdrSub.setText(Lang.t("كل الأقسام", "All Departments"));
        } else {
            Department d = Seed.findDept(AppState.session.deptId);
            tvHdrDept.setText(d != null ? d.label(Lang.isArabic()) : AppState.session.deptId);
            tvHdrSub.setText(Lang.t("إدارة الفنيين", "Technician Management"));
        }
    }

    private void showFragment(Fragment f) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragmentContainer, f);
        tx.commit();
    }

    private void reloadCurrentFragment() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (current != null) {
            FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
            tx.detach(current).attach(current);
            tx.commit();
        }
    }
}
