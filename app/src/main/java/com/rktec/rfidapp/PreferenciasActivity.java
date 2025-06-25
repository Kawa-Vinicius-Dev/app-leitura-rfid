package com.rktec.rfidapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import androidx.appcompat.app.AppCompatActivity;

public class PreferenciasActivity extends AppCompatActivity {

    private CheckBox cbConfirmacaoDupla, cbVibrar, cbAutoSalvar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferencias);

        cbConfirmacaoDupla = findViewById(R.id.cbConfirmacaoDupla);
        cbVibrar = findViewById(R.id.cbVibrar);
        cbAutoSalvar = findViewById(R.id.cbAutoSalvar);

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);

        // Carrega preferências salvas
        cbConfirmacaoDupla.setChecked(prefs.getBoolean("confirmacao_dupla", true));
        cbVibrar.setChecked(prefs.getBoolean("vibrar", false));
        cbAutoSalvar.setChecked(prefs.getBoolean("auto_salvar", false));

        cbConfirmacaoDupla.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("confirmacao_dupla", isChecked).apply()
        );
        cbVibrar.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("vibrar", isChecked).apply()
        );
        cbAutoSalvar.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("auto_salvar", isChecked).apply()
        );
    }
}
