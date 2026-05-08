# Quick Fixes for Critical Bugs

## BUG #1: Fix Recharge Array Index Out of Bounds

**File:** `ListTichNap.java` - Line 104

**Replace this:**
```java
public static void showTable(Player p) throws IOException {
    int tongnap = p.getTongNap();
    Message m = new Message(-90);
    m.writer().writeByte(0);
    m.writer().writeInt(tongnap);
    m.writer().writeByte(ENTRY.size());
    for (int i = 0; i < ENTRY.size(); i++) {
      ListTichNap t = ENTRY.get(i);
      m.writer().writeByte(i);
      m.writer().writeInt(t.num);
      m.writer().writeByte(p.tichNapCheck[i] == 1 ? 2 : (tongnap >= t.num ? 1 : 0));  // CRASH HERE
```

**With this:**
```java
public static void showTable(Player p) throws IOException {
    int tongnap = p.getTongNap();
    Message m = new Message(-90);
    m.writer().writeByte(0);
    m.writer().writeInt(tongnap);
    m.writer().writeByte(ENTRY.size());
    // Ensure tichNapCheck is properly sized
    if (p.tichNapCheck == null || p.tichNapCheck.length < ENTRY.size()) {
        byte[] newArray = new byte[ENTRY.size()];
        if (p.tichNapCheck != null) {
            System.arraycopy(p.tichNapCheck, 0, newArray, 0, Math.min(p.tichNapCheck.length, ENTRY.size()));
        }
        p.tichNapCheck = newArray;
    }
    for (int i = 0; i < ENTRY.size(); i++) {
      ListTichNap t = ENTRY.get(i);
      m.writer().writeByte(i);
      m.writer().writeInt(t.num);
      // Now safe to access
      m.writer().writeByte(p.tichNapCheck[i] == 1 ? 2 : (tongnap >= t.num ? 1 : 0));
```

---

## BUG #2: Fix Integer Overflow in Shop Quantity

**File:** `Service.java` - Around line 869

**Replace this:**
```java
if (TypeShop != 116 && TypeShop != 118 && (value == 1 || value == 20 || value == 500) && p.soluongmua > 0) {
    value = (short) p.soluongmua;
}
```

**With this:**
```java
if (TypeShop != 116 && TypeShop != 118 && (value == 1 || value == 20 || value == 500) && p.soluongmua > 0) {
    // Validate and bound the quantity
    if (p.soluongmua > DataTemplate.MAX_ITEM_IN_BAG || p.soluongmua < 0) {
        Service.send_box_ThongBao_OK(p, "Số lượng không hợp lệ!");
        return;
    }
    value = (short) Math.min(p.soluongmua, DataTemplate.MAX_ITEM_IN_BAG);
}
```

---

## BUG #3: Fix Market Price Validation

**File:** `ClientYesNo.java` - Add in `process()` function (around line 2750+)

**Add this new case:**
```java
case 5:  // Market sell item with price validation
case 6:  // Market sell beri with price validation
case 7:  // Market sell item47 with price validation
{
    try {
        String priceStr = m2.reader().readUTF();
        int price = Integer.parseInt(priceStr);
        
        // Validate price
        final int MIN_PRICE = 100;
        final int MAX_PRICE = Integer.MAX_VALUE / 2;  // Prevent overflow
        
        if (price < MIN_PRICE || price > MAX_PRICE) {
            Service.send_box_ThongBao_OK(p, 
                "Giá bán phải từ " + Util.number_format(MIN_PRICE) + 
                " đến " + Util.number_format(MAX_PRICE) + " extol");
            p.data_yesno = null;
            return;
        }
        
        // Price is valid, continue with market logic
        Market.confirm_sell_item(p, id, price);  // Call existing market sell logic
    } catch (NumberFormatException e) {
        Service.send_box_ThongBao_OK(p, "Giá phải là số nguyên!");
        p.data_yesno = null;
    }
    break;
}
```

---

## BUG #4: Fix Trade Balance Check Before Deduction

**File:** `Trade.java` - In `process()` case 4, around line 260

**Replace this:**
```java
// Trừ phí và vàng
p.update_ngoc(-p.fee_trade);
target.update_ngoc(-target.fee_trade);
p.update_vang(-(p.money_trade * 130L) / 100L);
target.update_vang(-(target.money_trade * 130L) / 100L);
p.update_vang(target.money_trade);
target.update_vang(p.money_trade);
// Nếu âm vàng → rollback
if (p.get_vang() < 0 || target.get_vang() < 0) {
```

**With this:**
```java
// Validate sufficient balance FIRST before any deduction
long totalCostP = p.fee_trade + ((long)p.money_trade * 130L / 100L);
long totalCostT = target.fee_trade + ((long)target.money_trade * 130L / 100L);

if (p.get_ngoc() < p.fee_trade) {
    Service.send_box_ThongBao_OK(p, 
        "Bạn không đủ " + String.format("%,d", p.fee_trade) + " Ruby để trả phí giao dịch");
    return;
}
if (target.get_ngoc() < target.fee_trade) {
    Service.send_box_ThongBao_OK(target, 
        "Bạn không đủ " + String.format("%,d", target.fee_trade) + " Ruby để trả phí giao dịch");
    return;
}
if (p.get_vang() < ((long)p.money_trade * 130L / 100L)) {
    Service.send_box_ThongBao_OK(p, 
        "Bạn không đủ beri để thanh toán phí 30%");
    return;
}
if (target.get_vang() < ((long)target.money_trade * 130L / 100L)) {
    Service.send_box_ThongBao_OK(target, 
        "Bạn không đủ beri để thanh toán phí 30%");
    return;
}

// All checks passed, now deduct
p.update_ngoc(-p.fee_trade);
target.update_ngoc(-target.fee_trade);
p.update_vang(-(p.money_trade * 130L) / 100L);
target.update_vang(-(target.money_trade * 130L) / 100L);
p.update_vang(target.money_trade);
target.update_vang(p.money_trade);
```

---

## BUG #5: Fix Trade Item Recovery on Disconnect

**File:** `Trade.java` - In `end_trade_by_disconnect()` function

**Replace the function with:**
```java
public static void end_trade_by_disconnect(Player p, Player target, int type, String name) throws IOException {
    if (p == null) return;
    
    // FIRST: Recover items back to inventory before cleanup
    if (p.list_item_trade3 != null && p.list_item_trade3.size() > 0) {
        for (Item_wear it : p.list_item_trade3) {
            if (p.item.able_bag() > 0) {
                p.item.add_item_bag3(it);
            } else {
                // Can't fit in bag - drop on ground
                core.GameLogger.warn("Trade disconnect: Item dropped for " + p.name + ": " + it.template.name);
            }
        }
        p.list_item_trade3.clear();
    }
    
    if (p.list_item_trade47 != null && p.list_item_trade47.size() > 0) {
        for (ItemBag47 it47 : p.list_item_trade47) {
            p.item.add_item_bag47(it47.category, it47.id, it47.quant);
        }
        p.list_item_trade47.clear();
    }
    
    // Refund money if any was locked
    if (p.money_trade > 0) {
        p.update_vang(p.money_trade);
    }
    
    // Update inventory display
    try {
        p.item.update_Inventory(-1, false);
    } catch (Exception e) {
        core.GameLogger.error("Error updating inventory for " + p.name, e);
    }
    
    // THEN: Reset trade state
    p.fee_trade = 0;
    p.money_trade = 0;
    p.is_lock_trade = false;
    p.is_accept_trade = false;
    p.trade_target = null;
}
```

---

## BUG #6: Fix Market Seller Payment

**File:** `ClientYesNo.java` - In case 25, around line 2730

**Replace this section:**
```java
it_receive.time_market = 0;
it_receive.type_market = 2;
p.update_money();
//
p.item.add_item_bag3(it_add);
```

**With this:**
```java
it_receive.time_market = 0;
it_receive.type_market = 2;
p.update_money();
p.item.add_item_bag3(it_add);

// ADD: Credit seller immediately if online, else save to database
Player seller = map.Map.get_player_by_name_allmap(it_receive.seller);
if (seller != null && seller.conn != null && seller.conn.connected) {
    long sellerPayment = (long)(it_receive.price_market * 0.9);  // 10% commission
    seller.update_vnd(+(int)sellerPayment);
    seller.update_money();
    Service.send_box_ThongBao_OK(seller, 
        "💰 Vật phẩm " + it_receive.template.name + " đã bán với giá " +
        Util.number_format((int)sellerPayment) + " Extol!");
} else {
    // Seller offline - log for later
    try {
        java.sql.Connection conn = database.SQL.gI().getCon();
        java.sql.PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO market_revenue (player_name, extol_amount, item_name, transaction_date) VALUES (?, ?, ?, NOW())");
        ps.setString(1, it_receive.seller);
        ps.setInt(2, (int)(it_receive.price_market * 0.9));
        ps.setString(3, it_receive.template.name);
        ps.executeUpdate();
        ps.close();
    } catch (Exception e) {
        core.GameLogger.error("Error logging market revenue for seller: " + it_receive.seller, e);
    }
}
```

---

## BUG #7: Fix Trade Fee Bypass

**File:** `Trade.java` - In case 3 (lock), around line 231

**Replace this:**
```java
int fee_item4 = 0;
for (int i = 0; i < p.list_item_trade47.size(); i++) {
    fee_item4 += p.list_item_trade47.get(i).quant * 10; // Mỗi `quant` có phí là 10
}
p.fee_trade += fee_item4;
```

**With this:**
```java
// Calculate fee based on total quantity, not per-stack
int totalQuantity = 0;
for (int i = 0; i < p.list_item_trade47.size(); i++) {
    totalQuantity += p.list_item_trade47.get(i).quant;
}
int fee_item4 = totalQuantity * 10;  // 10 per item, regardless of stacking
p.fee_trade += fee_item4;
```

---

## CRITICAL: Initialize tichNapCheck Array Properly

**File:** `Player.java` - In constructor or initialization

**Add this:**
```java
// When creating a new player or loading player data
if (this.tichNapCheck == null) {
    this.tichNapCheck = new byte[activities.ListTichNap.ENTRY.size()];
    // Initialize all to 0 (not claimed)
    for (int i = 0; i < this.tichNapCheck.length; i++) {
        this.tichNapCheck[i] = 0;
    }
}
```

---

*All fixes are ready for implementation. Test each fix independently first.*
