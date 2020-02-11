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

public class LibraryActivity extends AppCompatActivity {

    public static final String EXCHANGE_FANOUT = "fanoutExchange";
    public static final String EXCHANGE_DIRECT = "directExchange";
    public static final String EXCHANGE_TOPIC = "topicExchange";
    private final String uri = "192.168.1.117";
    private Thread generalSubscribeThread, economySubscribeThread, engineeringSubscribeThread, programmingSubscribeThread, publishingThread;
    private ListView listView;
    EditText sendMessageLibrary;
    private FloatingActionButton sendMessageButton;
    private CustomAdapter adapter;
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

                AlertDialog.Builder aBuilder = new AlertDialog.Builder(LibraryActivity.this);
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
        setContentView(R.layout.activity_library);
        Intent loginIntent = getIntent();

        login = loginIntent.getStringExtra("login");
        System.out.println("german1: Checking if the login was succesfully extracted from extras" + login);

        getWindow().setBackgroundDrawableResource(R.drawable.background);
        Toolbar toolbarLibrary = findViewById(R.id.toolbar);
        toolbarLibrary.setTitle("Library");
        setSupportActionBar(toolbarLibrary);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        selectGroupButton = findViewById(R.id.selectGroupButton);
        groupList = getResources().getStringArray(R.array.chatGroups);
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
                AlertDialog.Builder aBuilder = new AlertDialog.Builder(LibraryActivity.this);
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


        listView = findViewById(R.id.messagesListLibrary);
        sendMessageLibrary = findViewById(R.id.sendMessageLibrary);
        sendMessageButton = findViewById(R.id.sendButtonLibrary);
        adapter = new CustomAdapter(getApplicationContext());
        listView.setAdapter(adapter);

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
                            adapter.add(model);
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

                String message = "[" + login + "]" + "{" + chatGroups + "}" + sendMessageLibrary.getText().toString();
                if (!sendMessageLibrary.getText().toString().equals("")) {
                    publishMessage(message);
                    sendMessageLibrary.setText("");
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
                                channel.exchangeDeclare(EXCHANGE_FANOUT, "fanout");
                                channel.exchangeDeclare(EXCHANGE_DIRECT, "direct");

                                while (true) {
                                    String message = queue.takeFirst();
                                    String chatGroups = message.substring(message.indexOf("{") + 1, message.indexOf("}"));
                                    try {
                                        if (chatGroups.contains("General")) {
                                            channel.basicPublish(EXCHANGE_FANOUT, "", null, message.getBytes());
                                            System.out.println("==============: basic publish general was successful");
                                        }
                                        if (chatGroups.contains("Economy")) {
                                            channel.basicPublish(EXCHANGE_DIRECT, "Economy", null, message.getBytes());
                                            System.out.println("==============: basic publish ECONOMY was successful");
                                        }
                                        if (chatGroups.contains("Engineering")) {
                                            channel.basicPublish(EXCHANGE_DIRECT, "Engineering", null, message.getBytes());
                                            System.out.println("==============: basic publish ENGINEERING was successful");
                                        }
                                        if (chatGroups.contains("Programming")) {
                                            channel.basicPublish(EXCHANGE_DIRECT, "Programming", null, message.getBytes());
                                            System.out.println("==============: basic publish Programming was successful");
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

                    generalSubscribeThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Channel channel = connection.createChannel();
                                channel.basicQos(1);
                                channel.exchangeDeclare(EXCHANGE_FANOUT, "fanout");
                                String QUEUE_GENERAL = channel.queueDeclare().getQueue();
                                channel.queueBind(QUEUE_GENERAL, EXCHANGE_FANOUT, "");

                                QueueingConsumer consumer = new QueueingConsumer(channel);
                                channel.basicConsume(QUEUE_GENERAL, true, consumer);

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

                    economySubscribeThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Channel channel = connection.createChannel();
                                channel.basicQos(1);
                                channel.exchangeDeclare(EXCHANGE_DIRECT, "direct");
                                String QUEUE_ECONOMY = channel.queueDeclare().getQueue();
                                channel.queueBind(QUEUE_ECONOMY, EXCHANGE_DIRECT, "Economy");

                                QueueingConsumer consumer = new QueueingConsumer(channel);
                                channel.basicConsume(QUEUE_ECONOMY, true, consumer);

                                while (true) {
                                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                                    String parcel = new String(delivery.getBody());
                                    if ((boolean) (subGroups.get("Economy"))) {
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

                    engineeringSubscribeThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Channel channel = connection.createChannel();
                                channel.basicQos(1);
                                channel.exchangeDeclare(EXCHANGE_DIRECT, "direct");
                                String QUEUE_ENGINEERING = channel.queueDeclare().getQueue();
                                channel.queueBind(QUEUE_ENGINEERING, EXCHANGE_DIRECT, "Engineering");

                                QueueingConsumer consumer = new QueueingConsumer(channel);
                                channel.basicConsume(QUEUE_ENGINEERING, true, consumer);

                                while (true) {
                                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                                    String parcel = new String(delivery.getBody());
                                    if ((boolean) (subGroups.get("Engineering"))) {
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

                    programmingSubscribeThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Channel channel = connection.createChannel();
                                channel.basicQos(1);
                                channel.exchangeDeclare(EXCHANGE_DIRECT, "direct");
                                String QUEUE_PROGRAMMING = channel.queueDeclare().getQueue();
                                channel.queueBind(QUEUE_PROGRAMMING, EXCHANGE_DIRECT, "Programming");

                                QueueingConsumer consumer = new QueueingConsumer(channel);
                                channel.basicConsume(QUEUE_PROGRAMMING, true, consumer);

                                while (true) {
                                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                                    String parcel = new String(delivery.getBody());
                                    if ((boolean) (subGroups.get("Programming"))) {
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

                    generalSubscribeThread.start();
                    economySubscribeThread.start();
                    engineeringSubscribeThread.start();
                    programmingSubscribeThread.start();

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
        generalSubscribeThread.interrupt();
        economySubscribeThread.interrupt();
        engineeringSubscribeThread.interrupt();
        programmingSubscribeThread.interrupt();
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


