package com.rktec.rfidapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
        // --- MUDANÇA PRINCIPAL AQUI ---
        // Agora infla o nosso layout customizado 'item_lido.xml'
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lido, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemLeituraSessao sessao = itens.get(position);

        // Se o item foi encontrado na planilha
        if (sessao.encontrado && sessao.item != null) {
            holder.tvDescricao.setText(sessao.item.descresumida);
            String subtexto = "Plaqueta: " + sessao.epc + " | Local: " + sessao.item.codlocalizacao;
            holder.tvEp.setText(subtexto);

            // Muda o ícone e a cor com base no status
            holder.iconStatus.setImageResource(R.drawable.ic_check);
            holder.iconStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.success_green));

        } else { // Se o item NÃO foi encontrado (EPC novo)
            holder.tvDescricao.setText("Item não cadastrado");
            holder.tvEp.setText("EPC: " + sessao.epc);

            // Muda o ícone e a cor para indicar um problema/aviso
            holder.iconStatus.setImageResource(R.drawable.ic_close); // Supondo que você tenha um ícone de 'X'
            holder.iconStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.error_red));
        }
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // --- ATUALIZADO: Referências aos componentes do novo layout ---
        TextView tvDescricao;
        TextView tvEp;
        ImageView iconStatus;

        public ViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            // Encontra os novos componentes pelo ID
            tvDescricao = itemView.findViewById(R.id.tvDescricaoItem);
            tvEp = itemView.findViewById(R.id.tvEp);
            iconStatus = itemView.findViewById(R.id.icon_status);

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