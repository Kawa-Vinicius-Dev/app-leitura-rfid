package com.jvconsult.rfidapp;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.List;

public class ItemLeituraSessaoAdapter extends ArrayAdapter<ItemLeituraSessao> {

    public ItemLeituraSessaoAdapter(Context context, List<ItemLeituraSessao> itens) {
        super(context, 0, itens);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemLeituraSessao sessao = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        }
        TextView tv = (TextView) convertView.findViewById(android.R.id.text1);

        if (sessao.encontrado && sessao.item != null) {
            tv.setText(sessao.item.descresumida + " (" + sessao.item.codlocalizacao + ")");
        } else {
            tv.setText("Não encontrado: " + sessao.epc);
        }

        return convertView;
    }
}
