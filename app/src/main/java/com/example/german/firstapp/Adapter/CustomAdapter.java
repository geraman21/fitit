package com.example.german.firstapp.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.example.german.firstapp.Models.ChatModel;
import com.example.german.firstapp.Models.DuplicateChecker;
import com.example.german.firstapp.R;
import com.github.library.bubbleview.BubbleTextView;

import java.util.ArrayList;

public class CustomAdapter extends BaseAdapter {

    private ArrayList<ChatModel> chatModelList = new ArrayList<ChatModel>();
    private Context context;

    public CustomAdapter(Context context) {
        this.context = context;
    }

    public void add(ChatModel chatModel) {
        chatModelList.add(chatModel);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return chatModelList.size();
    }

    @Override
    public Object getItem(int position) {
        return chatModelList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ChatModel chatModel = chatModelList.get(position);

            if (chatModel.getSender().equals(chatModel.getUserLogin())) {
                convertView = layoutInflater.inflate(R.layout.list_item_message_send, null);
                BubbleTextView bubbleTextView = (BubbleTextView) convertView.findViewById(R.id.text_message);
                TextView time = (TextView) convertView.findViewById(R.id.text_message_time);
                TextView chatGroups = (TextView) convertView.findViewById(R.id.publishedToList);
                time.setText(chatModel.getTime());
                chatGroups.setText(chatModel.getPublishGroups());
                bubbleTextView.setTextColor(Color.BLACK);
                bubbleTextView.setText(chatModel.getMessage());
            } else {
                convertView = layoutInflater.inflate(R.layout.list_item_message_receive, null);
                BubbleTextView bubbleTextView = (BubbleTextView) convertView.findViewById(R.id.text_message);
                TextView time = (TextView) convertView.findViewById(R.id.text_message_time);
                TextView login = (TextView) convertView.findViewById(R.id.text_message_name);
                TextView chatGroups = (TextView) convertView.findViewById(R.id.publishedAtList);
                chatGroups.setText(chatModel.getPublishGroups());
                time.setText(chatModel.getTime());
                login.setText(chatModel.getSender());
                bubbleTextView.setTextColor(Color.BLACK);
                bubbleTextView.setText(chatModel.getMessage());
            }

        return convertView;
    }
}
