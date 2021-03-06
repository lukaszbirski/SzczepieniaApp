package pl.students.szczepieniaapp.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import pl.students.szczepieniaapp.R
import pl.students.szczepieniaapp.databinding.OrderItemBinding
import pl.students.szczepieniaapp.domain.model.Order

class OrderAdapter(
    private val orders: List<Order>,
    private val listener: OrderAdapterListener,
    private val isRemoveItemVisible: Boolean
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        OrderViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.order_item,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.recyclerItem.apply {
            order = orders[position]
        }
        holder.recyclerItem.removeItem.setOnClickListener {
            listener.removeItem(holder.recyclerItem.root, orders[position])
        }
        if (isRemoveItemVisible) holder.recyclerItem.removeItem.visibility = View.VISIBLE else holder.recyclerItem.removeItem.visibility = View.GONE
    }

    override fun getItemCount() = orders.size

    inner class OrderViewHolder(
        val recyclerItem: OrderItemBinding) : RecyclerView.ViewHolder(recyclerItem.root)
}