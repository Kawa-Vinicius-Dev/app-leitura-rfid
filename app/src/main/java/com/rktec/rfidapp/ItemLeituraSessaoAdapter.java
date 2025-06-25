package com.rktec.rfidapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ItemLeituraSessaoAdapter extends RecyclerView.Adapter<ItemLeituraSessaoAdapter.ViewHolder> {

    private List<ItemLeituraSessao> itens;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ItemLeituraSessaoAdapter(List<ItemLeituraSessao> itens) {
        this.itens = itens;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemLeituraSessao sessao = itens.get(position);
        if (sessao.encontrado && sessao.item != null) {
            holder.tv.setText(sessao.item.descresumida + " (" + sessao.item.codlocalizacao + ")");
        } else {
            holder.tv.setText("Não encontrado: " + sessao.epc);
        }
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv;
        public ViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            tv = itemView.findViewById(android.R.id.text1);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION)
                        listener.onItemClick(pos);
                }
            });
        }
    }
}
