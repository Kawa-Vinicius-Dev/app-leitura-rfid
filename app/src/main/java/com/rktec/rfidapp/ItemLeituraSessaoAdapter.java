package com.rktec.rfidapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ItemLeituraSessaoAdapter extends RecyclerView.Adapter<ItemLeituraSessaoAdapter.ViewHolder> {

    private List<ItemLeituraSessao> itens;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private OnItemClickListener listener;

    public ItemLeituraSessaoAdapter(List<ItemLeituraSessao> itens) {
        this.itens = itens;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lido, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemLeituraSessao sessao = itens.get(position);

        // Quando existe um item associado (encontrado na base ou cadastrado manualmente)
        if (sessao.item != null) {
            String desc = (sessao.item.descresumida != null && !sessao.item.descresumida.isEmpty())
                    ? sessao.item.descresumida
                    : "Item encontrado";
            holder.tvDescricao.setText(desc);

            String local = (sessao.item.codlocalizacao != null && !sessao.item.codlocalizacao.isEmpty())
                    ? sessao.item.codlocalizacao
                    : "-";
            String subtexto = "Plaqueta: " + sessao.epc + " | Local: " + local;
            holder.tvEp.setText(subtexto);

            // Ícone e cor de acordo com o status
            switch (sessao.status) {
                case ItemLeituraSessao.STATUS_OK:
                    // Loja e setor corretos → verde OK
                    holder.iconStatus.setImageResource(R.drawable.ic_check);
                    holder.iconStatus.setColorFilter(Color.parseColor("#4CAF50"));
                    break;

                case ItemLeituraSessao.STATUS_SETOR_ERRADO:
                    // Loja correta, setor diferente → amarelo
                    holder.iconStatus.setImageResource(R.drawable.ic_check);
                    holder.iconStatus.setColorFilter(Color.parseColor("#FFC107"));
                    break;

                case ItemLeituraSessao.STATUS_LOJA_ERRADA:
                    // Item existe mas está em outra loja → ícone de loja, laranja
                    holder.iconStatus.setImageResource(R.drawable.ic_store);
                    holder.iconStatus.setColorFilter(Color.parseColor("#FF9800"));
                    break;

                case ItemLeituraSessao.STATUS_NAO_ENCONTRADO:
                default:
                    // Segurança: se marcar como não encontrado, trata como erro
                    holder.iconStatus.setImageResource(R.drawable.ic_close);
                    holder.iconStatus.setColorFilter(Color.parseColor("#F44336"));
                    break;
            }
        } else {
            // EPC não existe na base → item não cadastrado
            holder.tvDescricao.setText("Item não cadastrado");
            holder.tvEp.setText("EPC: " + sessao.epc);

            holder.iconStatus.setImageResource(R.drawable.ic_close);
            holder.iconStatus.setColorFilter(Color.parseColor("#F44336"));
        }
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescricao;
        TextView tvEp;
        ImageView iconStatus;

        public ViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            tvDescricao = itemView.findViewById(R.id.tvDescricaoItem);
            tvEp = itemView.findViewById(R.id.tvEp);
            iconStatus = itemView.findViewById(R.id.icon_status);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onItemClick(pos);
                    }
                }
            });
        }
    }
}
