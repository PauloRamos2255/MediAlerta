package com.pauloramos.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder> {

    private List<Medication> medicationList;

    public MedicationAdapter(List<Medication> medicationList) {
        this.medicationList = medicationList;
    }

    @NonNull
    @Override
    public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medication, parent, false);
        return new MedicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
        Medication medication = medicationList.get(position);

        holder.nameTextView.setText(medication.getName());
        holder.doseTextView.setText(medication.getDose() + "mg");
        holder.timeTextView.setText(medication.getTime());
        String status = medication.getRepeat();
        holder.statusTextView.setText(status);
        int backgroundColor;
        int textColor;
        switch (status) {
            case "Tomado":
                backgroundColor = holder.itemView.getContext().getColor(R.color.green_status); // Define estos colores en colors.xml
                textColor = holder.itemView.getContext().getColor(R.color.white);
                break;
            case "Pendiente":
                backgroundColor = holder.itemView.getContext().getColor(R.color.orange_status);
                textColor = holder.itemView.getContext().getColor(R.color.white);
                break;
            case "Saltado":
                backgroundColor = holder.itemView.getContext().getColor(R.color.red_status);
                textColor = holder.itemView.getContext().getColor(R.color.white);
                break;
            default:
                backgroundColor = holder.itemView.getContext().getColor(R.color.default_status); // Color por defecto
                textColor = holder.itemView.getContext().getColor(R.color.black);
                break;
        }

        holder.statusTextView.setTextColor(textColor);
    }

    @Override
    public int getItemCount() {
        return medicationList.size();
    }

    public void updateMedications(List<Medication> newMedicationList) {
        this.medicationList = newMedicationList;
        notifyDataSetChanged();
    }


    public static class MedicationViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView doseTextView;
        TextView timeTextView;
        TextView statusTextView;

        public MedicationViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.medicationNameTextView);
            doseTextView = itemView.findViewById(R.id.medicationDoseTextView);
            timeTextView = itemView.findViewById(R.id.medicationTimeTextView);
            statusTextView = itemView.findViewById(R.id.medicationStatusTextView);
        }
    }
}
