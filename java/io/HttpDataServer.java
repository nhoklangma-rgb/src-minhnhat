package io;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import core.GameLogger;
import template.DataTemplate;
import template.ItemOptionTemplate;
import template.ItemTemplate3;
import template.ItemTemplate4;
import template.ItemTemplate7;
import template.ItemTemplate8;
import template.MobTemplate;
import activities.UpgradeItem;
import template.DataUpgrade;
import core.Util;
import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class HttpDataServer {
    private HttpServer server;

    public void start(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/data/", new DataHandler());
            server.setExecutor(Executors.newFixedThreadPool(4)); // creates a default executor
            server.start();
            GameLogger.info("HTTP Data Server started on port " + port);
        } catch (IOException e) {
            GameLogger.error("Failed to start HTTP Data Server", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            GameLogger.info("HTTP Data Server stopped");
        }
    }

    static class DataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Enable CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Cache-Control", "public, max-age=86400");
            
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 3) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            int type;
            try {
                type = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            try {
                byte[] responseData = generateData(type);
                if (responseData == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }

                exchange.sendResponseHeaders(200, responseData.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseData);
                os.close();
            } catch (Exception e) {
                GameLogger.error("Error serving HTTP data type: " + type, e);
                exchange.sendResponseHeaders(500, -1);
            }
        }

        private byte[] generateData(int type) throws IOException {
            Message m2 = new Message(-7);
            map.f f = new map.f();
            switch (type) {
                case 12: {
                    m2.writer().writeByte(12);
                    m2.writer().writeByte(UpgradeItem.DATA.size());
                    for (int i = 0; i < UpgradeItem.DATA.size(); i++) {
                        DataUpgrade temp = UpgradeItem.DATA.get(i);
                        m2.writer().writeByte(temp.level);
                        m2.writer().writeShort(temp.per);
                        m2.writer().writeByte(temp.prelevel);
                        m2.writer().writeInt(temp.beri);
                        m2.writer().writeInt(temp.beri_white);
                        m2.writer().writeShort(temp.ruby);
                        m2.writer().writeShort(temp.att);
                        m2.writer().writeByte(temp.material.length);
                        for (int j = 0; j < temp.material.length; j++) {
                            m2.writer().writeByte(temp.material[j].type);
                            m2.writer().writeByte(temp.material[j].id);
                            m2.writer().writeShort(temp.material[j].quant);
                        }
                    }
                    m2.writer().writeShort(DataTemplate.VerdataUpgradeSave);
                    break;
                }
                case 13: {
                    m2.writer().writeByte(13);
                    m2.writer().writeByte(DataTemplate.mSea.length);
                    for (int i = 0; i < DataTemplate.mSea.length; i++) {
                        for (int j = 0; j < DataTemplate.mSea[i].length; j++) {
                            m2.writer().writeShort(DataTemplate.mSea[i][j]);
                        }
                    }
                    break;
                }
                case 15: {
                    m2.writer().writeByte(15);
                    m2.writer().writeShort(MobTemplate.ENTRYS.size());
                    for (int i = 0; i < MobTemplate.ENTRYS.size(); i++) {
                        MobTemplate temp = MobTemplate.ENTRYS.get(i);
                        m2.writer().writeShort(temp.mob_id);
                        m2.writer().writeUTF(temp.name);
                        m2.writer().writeShort(temp.level);
                        m2.writer().writeShort(temp.hOne);
                        m2.writer().writeInt(f.setInteger(temp.hp_max));
                        m2.writer().writeByte(temp.typemove);
                        m2.writer().writeByte(temp.ishuman);
                        m2.writer().writeByte(temp.typemonster);
                        if (temp.ishuman == 1) {
                            m2.writer().writeShort(temp.head);
                            m2.writer().writeShort(temp.hair);
                            m2.writer().writeByte(temp.wearing.length);
                            for (int j = 0; j < temp.wearing.length; j++) {
                                if (temp.wearing[j] != -1) {
                                    m2.writer().writeByte(1);
                                    m2.writer().writeShort(temp.wearing[j]);
                                } else {
                                    m2.writer().writeByte(-1);
                                }
                            }
                        } else {
                            m2.writer().writeShort(temp.icon);
                        }
                    }
                    m2.writer().writeShort(DataTemplate.VerdataMon);
                    break;
                }
                case 19: {
                    m2.writer().writeByte(19);
                    m2.writer().writeByte(DataTemplate.mTileUpdate.length);
                    for (int i = 0; i < DataTemplate.mTileUpdate.length; i++) {
                        m2.writer().writeShort(DataTemplate.mTileUpdate[i]);
                    }
                    m2.writer().writeByte(DataTemplate.mTileGhepĐa.length);
                    for (int i = 0; i < DataTemplate.mTileGhepĐa.length; i++) {
                        m2.writer().writeShort(DataTemplate.mTileGhepĐa[i]);
                    }
                    break;
                }
                case 21: {
                    m2.writer().writeByte(21);
                    m2.writer().writeByte(0); // h12plus = 0;
                    break;
                }
                case 26: {
                    m2.writer().writeByte(26);
                    m2.writer().writeByte(DataTemplate.AttriKichAn.length);
                    for (int i = 0; i < DataTemplate.AttriKichAn.length; i++) {
                        m2.writer().writeUTF(DataTemplate.AttriKichAn[i]);
                    }
                    m2.writer().writeShort(-31525);
                    break;
                }
                case 28: {
                    m2.writer().writeByte(28);
                    m2.writer().writeShort(ItemTemplate4.ENTRYS.size());
                    for (int i = 0; i < ItemTemplate4.ENTRYS.size(); i++) {
                        ItemTemplate4 temp = ItemTemplate4.ENTRYS.get(i);
                        m2.writer().writeShort(temp.id);
                        m2.writer().writeShort(temp.icon);
                        m2.writer().writeUTF(temp.name);
                        m2.writer().writeShort(temp.indexInfoPotion);
                        m2.writer().writeInt(temp.beri);
                        m2.writer().writeShort(temp.ruby);
                        m2.writer().writeByte(temp.istrade);
                        m2.writer().writeByte(temp.type);
                        m2.writer().writeShort(temp.timedelay);
                        m2.writer().writeShort(temp.value);
                        m2.writer().writeShort(temp.timeactive);
                        m2.writer().writeUTF(temp.nameuse);
                    }
                    m2.writer().writeShort(DataTemplate.VerdataPotion);
                    break;
                }
                case 27: {
                    m2.writer().writeByte(27);
                    m2.writer().writeByte(0); // isopenDao
                    break;
                }
                case 18:
                case 29: {
                    m2.writer().writeByte(18); // luon gui 18 theo goc
                    m2.writer().writeShort(ItemTemplate8.ENTRYS.size());
                    for (int i = 0; i < ItemTemplate8.ENTRYS.size(); i++) {
                        ItemTemplate8 temp = ItemTemplate8.ENTRYS.get(i);
                        m2.writer().writeShort(temp.id);
                        m2.writer().writeShort(temp.icon);
                        m2.writer().writeUTF(temp.name);
                        m2.writer().writeUTF(temp.info);
                        m2.writer().writeInt(temp.beri);
                        m2.writer().writeShort(temp.ruby);
                        m2.writer().writeByte(temp.istrade);
                        m2.writer().writeByte(temp.type);
                        m2.writer().writeShort(temp.timedelay);
                        m2.writer().writeShort(temp.value);
                        m2.writer().writeShort(temp.timeactive);
                        m2.writer().writeUTF(temp.nameuse);
                    }
                    m2.writer().writeShort(DataTemplate.VerdataPotionClan);
                    break;
                }
                case 30: {
                    m2.writer().writeByte(30);
                    m2.writer().writeByte(DataTemplate.mEffSpec.length);
                    for (int i = 0; i < DataTemplate.mEffSpec.length; i++) {
                        m2.writer().writeUTF(DataTemplate.mEffSpec[i]);
                    }
                    m2.writer().writeShort(-7547);
                    break;
                }
                case 4: {
                    m2.writer().writeByte(4);
                    m2.writer().writeByte(DataTemplate.mLockMap.length);
                    for (int i = 0; i < DataTemplate.mLockMap.length; i++) {
                        m2.writer().writeByte(DataTemplate.mLockMap[i]);
                    }
                    break;
                }
                case 8: {
                    m2.writer().writeByte(8);
                    for (int i = 0; i < DataTemplate.TabInventory_ItemSell.length; i++) {
                        m2.writer().writeShort(DataTemplate.TabInventory_ItemSell[i]);
                    }
                    break;
                }
                case 10: {
                    m2.writer().writeByte(10);
                    m2.writer().writeByte(DataTemplate.mMapLang.length);
                    for (int i = 0; i < DataTemplate.mMapLang.length; i++) {
                        m2.writer().writeShort(DataTemplate.mMapLang[i]);
                    }
                    break;
                }
                case 2: {
                    m2.writer().writeByte(2);
                    m2.writer().writeShort(ItemOptionTemplate.ENTRYS.size());
                    for (int i = 0; i < ItemOptionTemplate.ENTRYS.size(); i++) {
                        ItemOptionTemplate temp = ItemOptionTemplate.ENTRYS.get(i);
                        m2.writer().writeUTF(temp.name);
                        m2.writer().writeByte(temp.color);
                        m2.writer().writeByte(temp.percent);
                    }
                    m2.writer().writeShort(DataTemplate.VerdataAttri);
                    break;
                }
                case 6: {
                    m2.writer().writeByte(6);
                    File f_map = new File("data/msg/login/request/msg-7_6");
                    if (f_map.exists()) {
                        FileInputStream fis = new FileInputStream(f_map);
                        byte[] ab = new byte[fis.available() - 2];
                        fis.read(ab);
                        fis.close();
                        m2.writer().write(ab);
                        m2.writer().writeShort(DataTemplate.VerdataNameMap);
                    }
                    break;
                }
                case 7: {
                    m2.writer().writeByte(7);
                    m2.writer().writeShort(DataTemplate.NamePotionquest.length);
                    for (int i = 0; i < DataTemplate.NamePotionquest.length; i++) {
                        m2.writer().writeUTF(DataTemplate.NamePotionquest[i]);
                    }
                    m2.writer().writeShort(DataTemplate.VerdataNamePotionquest);
                    break;
                }
                case 11: {
                    m2.writer().writeByte(11);
                    m2.writer().writeByte(ItemTemplate7.ENTRYS.size());
                    for (int i = 0; i < ItemTemplate7.ENTRYS.size(); i++) {
                        ItemTemplate7 temp = ItemTemplate7.ENTRYS.get(i);
                        m2.writer().writeByte(temp.id);
                        m2.writer().writeUTF(temp.name);
                        m2.writer().writeByte(temp.type);
                        m2.writer().writeByte(temp.icon);
                        m2.writer().writeInt(temp.price);
                        m2.writer().writeShort(temp.priceruby);
                        m2.writer().writeByte(temp.istrade);
                    }
                    break;
                }
                default:
                    m2.cleanup();
                    return null;
            }
            byte[] data = m2.getData();
            m2.cleanup();
            return data;
        }
    }
}
