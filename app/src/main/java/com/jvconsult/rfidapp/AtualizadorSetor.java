package com.jvconsult.rfidapp;

import android.content.Context;
import android.widget.Toast;
import java.util.List;

public class AtualizadorSetor {
    public static void atualizar(Context context, List<ItemPlanilha> itens, String epc, SetorLocalizacao setor) {
        for (ItemPlanilha item : itens) {
            if (item.nroplaqueta.equals(epc)) {
                item.codlocalizacao = setor.codlocalizacao;
                Toast.makeText(context, "Item " + item.nroplaqueta + " movido para setor " + setor.setor, Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }
}
