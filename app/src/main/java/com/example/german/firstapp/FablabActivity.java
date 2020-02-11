package com.example.german.firstapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import com.example.german.firstapp.Adapter.CustomAdapter;
import com.example.german.firstapp.Models.ChatModel;
import com.rabbitmq.client.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class FablabActivity extends AppCompatActivity {

    public static final String EXCHANGE_FANOUT = "fanoutExchange";
    public static final String EXCHANGE_DIRECT = "directExchange";
    public static final String EXCHANGE_TOPIC = "topicExchange";
    private final String uri = "192.168.1.117";
    private Thread broadcastAllSubscribeThread, printSubscribeThread, dronesSubscribeThread, otherSubscribeThread, publishingThread;
    private ListView listView;
    EditText sendMessageFablab;
    private FloatingActionButton sendMessageButton;
    private CustomAdapter fablabAdapter;
    private boolean doubleBackToExitPressedOnce = false;
    ImageButton selectGroupButton;
    private boolean[] checkedGroups, checkedSubGroups;
    private String[] groupList;
    private HashMap subGroups;
    HashMap groups;
    ConnectionFactory factory = new ConnectionFactory();
    private String login;


    //============================================================================================================ConnectionSetup===============================================
    private BlockingDeque<String> queue = new LinkedBlockingDeque<String>();   //Message Queue

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.subscriptions:

                AlertDialog.Builder aBuilder = new AlertDialog.Builder(FablabActivity.this);
                aBuilder.setTitle("Select chat Groups to subscribe for");
                aBuilder.setMultiChoiceItems(groupList, checkedSubGroups, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        subGroups.put(groupList[which], isChecked);
                    }
                });
                aBuilder.setCancelable(true);
                AlertDialog dialog = aBuilder.create();
                dialog.show();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fablab);
        Intent loginIntent = getIntent();

        login = loginIntent.getStringExtra("login");
        System.out.println("german1: Checking if the login was succesfully extracted from extras" + login);

        getWindow().setBackgroundDrawableResource(R.drawable.background);
        Toolbar toolbarFablab = findViewById(R.id.toolbar);
        toolbarFablab.setTitle("Fablab");
        setSupportActionBar(toolbarFablab);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        selectGroupButton = findViewById(R.id.selectGroupButton);
        groupList = getResources().getStringArray(R.array.chatGroupsFablab);
        checkedGroups = new boolean[groupList.length];
        groups = new HashMap();
        for (int i = 0; i < checkedGroups.length; i++) {
            groups.put(groupList[i], false);
            checkedGroups[i] = false;
        }
        groups.put(groupList[0], true);
        checkedGroups[0] = true;

        selectGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder aBuilder = new AlertDialog.Builder(FablabActivity.this);
                aBuilder.setTitle("Select chat Groups to publish your messages");
                aBuilder.setMultiChoiceItems(groupList, checkedGroups, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        groups.put(groupList[which], isChecked);
                    }
                });
                aBuilder.setCancelable(true);
                AlertDialog dialog = aBuilder.create();
                dialog.show();
                System.out.println("german1: " + " " + groups.keySet().toString() + groups.values().toString());
            }
        });


        checkedSubGroups = new boolean[groupList.length];
        subGroups = new HashMap();
        for (int i = 0; i < checkedSubGroups.length; i++) {
            subGroups.put(groupList[i], false);
            checkedSubGroups[i] = false;
        }
        subGroups.put(groupList[0], true);
        checkedSubGroups[0] = true;


        listView = findViewById(R.id.messagesListFablab);
        sendMessageFablab = findViewById(R.id.sendMessageFablab);
        sendMessageButton = findViewById(R.id.sendButtonFablab);
        fablabAdapter = new CustomAdapter(getApplicationContext());
        listView.setAdapter(fablabAdapter);

        setupConnectionFactory(factory);
        publishMessageToRabbit();
        setupSendButton();

        final Handler incomingMessageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String parcel = msg.getData().getString("msg");
                String message = parcel.substring(parcel.indexOf("}") + 1);
                String sender = parcel.substring(1, parcel.indexOf("]"));
                String publishGroups = parcel.substring(parcel.indexOf("{") + 1, parcel.indexOf("}"));
                System.out.println("german1: " + parcel);
                Date now = new Date();
                SimpleDateFormat ft = new SimpleDateFormat("hh:mm");
                ChatModel model = new ChatModel(message, ft.format(now), sender, login, publishGroups);

                System.out.println("================================" + login + " =================================================");
                updateMessages(model);
            }
        };

        subscribe(incomingMessageHandler);

    }

    void updateMessages(final ChatModel model) {
        final ChatModel chatModel = model;

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fablabAdapter.add(model);
                            listView.setSelection(listView.getCount() - 1);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    void setupSendButton() {

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String chatGroups = "";
                for (int i = 0; i < groupList.length; i++) {
                    if (Boolean.parseBoolean(groups.get(groupList[i]).toString())) {
                        chatGroups = chatGroups + groupList[i] + " ,";
                    }
                }

                System.out.println("german1: " + chatGroups);

                String message = "[" + login + "]" + "{" + chatGroups + "}" + sendMessageFablab.getText().toString();
                if (!sendMessageFablab.getText().toString().equals("")) {
                    publishMessage(message);
                    sendMessageFablab.setText("");
                }
            }
        });
    }

    private void setupConnectionFactory(final ConnectionFactory factory) {
        Thread factoryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                factory.setUsername("hsrw");
                factory.setPassword("123456");
                factory.setHost(uri);
                System.out.println("german1: factory setup was successful");

            }
        });
        factoryThread.start();
    }

    void publishMessage(String message) {
        try {
            queue.putLast(message);
        } catch (InterruptedException e) {
            System.out.println("Error1: exception in putting the message into the queue, line 43");
        }
    }

    public void publishMessageToRabbit() {

        Thread connectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Connection connection = factory.newConnection();

                    publishingThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {

                                Channel channel = connection.createChannel();
                                channel.exchangeDeclare(EXCHANGE_TOPIC, "topic");

                                while (true) {
                                    String message = queue.takeFirst();
                                    String chatGroups = message.substring(message.indexOf("{") + 1, message.indexOf("}"));
                                    try {
                                        if (chatGroups.contains("BroadcastAll")) {
                                            channel.basicPublish(EXCHANGE_TOPIC, "General", null, message.getBytes());
                                            System.out.println("==============: basic publish broadcastAll was successful");
                                        }
                                        if (chatGroups.contains("3DPrint")) {
                                            channel.basicPublish(EXCHANGE_TOPIC, "3DPrint", null, message.getBytes());
                                            System.out.println("==============: basic publish 3DPrint was successful");
                                        }
                                        if (chatGroups.contains("Drones")) {
                                            channel.basicPublish(EXCHANGE_TOPIC, "Drones", null, message.getBytes());
                                            System.out.println("==============: basic publish Drones was successful");
                                        }
                                        if (chatGroups.contains("Other")) {
                                            channel.basicPublish(EXCHANGE_TOPIC, "Other", null, message.getBytes());
                                            System.out.println("==============: basic publish Other was successful");
                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    });
                    publishingThread.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
        connectionThread.start();
    }

    void subscribe(final Handler handler) {
        Thread mainSubscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Connection connection = factory.newConnection();

                    broadcastAllSubscribeThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Channel channel = connection.createChannel();
                                channel.basicQos(1);
                                channel.exchangeDeclare(EXCHANGE_TOPIC, "topic");
                                String QUEUE_BROADCASTALL = channel.queueDeclare().getQueue();
                                channel.queueBind(QUEUE_BROADCASTALL, EXCHANGE_TOPIC, "#");

                                QueueingConsumer consumer = new QueueingConsumer(channel);
                                channel.basicConsume(QUEUE_BROADCASTALL, true, consumer);

                                while (true) {
                                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                                    String parcel = new String(delivery.getBody());
                                    if ((boolean) (subGroups.get("General"))) {
                                        Message msg = handler.obtainMessage();
                                        Bundle bundle = new Bundle();
                                        bundle.putString("msg", parcel);
                                        msg.setData(bundle);
                                        handler.sendMessage(msg);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    printSubscribeThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Channel channel = connection.createChannel();
                                channel.basicQos(1);
                                channel.exchangeDeclare(EXCHANGE_TOPIC, "topic");
                                String QUEUE_3DPRINT = channel.queueDeclare().getQueue();
                                channel.queueBind(QUEUE_3DPRINT, EXCHANGE_TOPIC, "3DPrint");

                                QueueingConsumer consumer = new QueueingConsumer(channel);
                                channel.basicConsume(QUEUE_3DPRINT, true, consumer);

                                while (true) {
                                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                                    String parcel = new String(delivery.getBody());
                                    if ((boolean) (subGroups.get("3DPrint"))) {
                                        Message msg = handler.obtainMessage();
                                        Bundle bundle = new Bundle();
                                        bundle.putString("msg", parcel);
                                        msg.setData(bundle);
                                        handler.sendMessage(msg);
                                    }
                                }
                            } catch (
                                    Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    dronesSubscribeThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Channel channel = connection.createChannel();
                                channel.basicQos(1);
                                channel.exchangeDeclare(EXCHANGE_TOPIC, "topic");
                                String QUEUE_DRONES = channel.queueDeclare().getQueue();
                                channel.queueBind(QUEUE_DRONES, EXCHANGE_TOPIC, "Drones");

                                QueueingConsumer consumer = new QueueingConsumer(channel);
                                channel.basicConsume(QUEUE_DRONES, true, consumer);

                                while (true) {
                                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                                    String parcel = new String(delivery.getBody());
                                    if ((boolean) (subGroups.get("Drones"))) {
                                        Message msg = handler.obtainMessage();
                                        Bundle bundle = new Bundle();
                                        bundle.putString("msg", parcel);
                                        msg.setData(bundle);
                                        handler.sendMessage(msg);
                                    }
                                }
                            } catch (
                                    Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    otherSubscribeThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Channel channel = connection.createChannel();
                                channel.basicQos(1);
                                channel.exchangeDeclare(EXCHANGE_TOPIC, "topic");
                                String QUEUE_OTHER = channel.queueDeclare().getQueue();
                                channel.queueBind(QUEUE_OTHER, EXCHANGE_TOPIC, "Fablab.Other");

                                QueueingConsumer consumer = new QueueingConsumer(channel);
                                channel.basicConsume(QUEUE_OTHER, true, consumer);

                                while (true) {
                                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                                    String parcel = new String(delivery.getBody());
                                    if ((boolean) (subGroups.get("Other"))) {
                                        Message msg = handler.obtainMessage();
                                        Bundle bundle = new Bundle();
                                        bundle.putString("msg", parcel);
                                        msg.setData(bundle);
                                        handler.sendMessage(msg);
                                    }
                                }
                            } catch (
                                    Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    broadcastAllSubscribeThread.start();
                    printSubscribeThread.start();
                    dronesSubscribeThread.start();
                    otherSubscribeThread.start();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mainSubscribeThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        publishingThread.interrupt();
        broadcastAllSubscribeThread.interrupt();
        printSubscribeThread.interrupt();
        dronesSubscribeThread.interrupt();
        otherSubscribeThread.interrupt();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }
}


