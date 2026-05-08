# Bug Report - Market, Trade, Shop Nguyên Liệu, Mốc Nạp

**Generated:** May 8, 2026  
**Analysis Scope:** Market.java, Trade.java, ListTichNap.java, Service.java, ClientYesNo.java

---

## CRITICAL BUGS

### 🔴 BUG #1: Market - No Item Price Validation
**Severity:** CRITICAL  
**File:** [Market.java](Market.java#L269-L326)  
**Impact:** Players can list items at exploitative prices (0 extol, 999B extol)

**Problem:**
```java
// No validation of price input
p.data_yesno = new int[] { id };
Service.input_text(p, 5, "Đăng bán", new String[] { "Giá bán" });
```
When player enters price via `input_text`, the value goes directly to `ClientYesNo` without any bounds checking.

**Risk:**
- Player A sells item for 1 extol (should be 100K extol)
- Massive economic loss possible
- Could trigger cascading inflation/deflation exploits

**Recommendation:**
```java
// Add price validation in ClientYesNo.java when processing price input
if (price < MIN_MARKET_PRICE || price > MAX_MARKET_PRICE) {
    Service.send_box_ThongBao_OK(p, "Giá bán phải từ " + MIN_MARKET_PRICE + " đến " + MAX_MARKET_PRICE + " extol");
    return;
}
```

---

### 🔴 BUG #2: Recharge Milestone - Array Index Out of Bounds
**Severity:** CRITICAL  
**File:** [ListTichNap.java](ListTichNap.java#L104)  
**Impact:** Game crash when checking recharge milestone progress

**Problem:**
```java
m.writer().writeByte(p.tichNapCheck[i] == 1 ? 2 : (tongnap >= t.num ? 1 : 0));
```
No validation that `p.tichNapCheck` array is sized correctly before indexing.

**Code Flow:**
1. Player loads recharge milestone UI
2. System loops through all ENTRY.size() milestones
3. If `p.tichNapCheck.length < ENTRY.size()`, crash occurs
4. ArrayIndexOutOfBoundsException not caught

**Recommendation:**
```java
// Validate array size before access
if (i >= p.tichNapCheck.length) {
    m.writer().writeByte(tongnap >= t.num ? 1 : 0);  // Default: not claimed
} else {
    m.writer().writeByte(p.tichNapCheck[i] == 1 ? 2 : (tongnap >= t.num ? 1 : 0));
}
```

---

### 🔴 BUG #3: Shop - Item Stock Unlimited (Critical Resource Drain)
**Severity:** CRITICAL  
**File:** [Service.java](Service.java#L850-L900)  
**Impact:** Infinite item supply creates economic exploit

**Problem:**
```java
// In buy_item function - no inventory management
p.update_vang(-temp_sell.price);
p.update_money();
Item_wear it_add = new Item_wear();
it_add.setup_template_by_id(temp_sell.id);
if (it_add.template != null) {
    p.item.add_item_bag3(it_add);  // Creates item from thin air
}
```
Shop items are generated infinitely without stock tracking.

**Risk:**
- 1000 players buy same item → 1000 copies created
- No vendor inventory limit
- Currency drain if items have Extol cost but no corresponding sink

**Recommendation:**
```java
// Add inventory tracking
ShopInventory inv = ShopInventory.get_inventory(temp_sell.id);
if (inv.quantity <= 0) {
    Service.send_box_ThongBao_OK(p, "Vật phẩm hết hàng!");
    return;
}
inv.quantity--;
// ... proceed with purchase
```

---

## HIGH SEVERITY BUGS

### 🟠 BUG #4: Market - Race Condition on Purchase
**Severity:** HIGH  
**File:** [ClientYesNo.java](ClientYesNo.java#L2691-L2750)  
**Impact:** Item sold twice to different buyers, or seller can hijack purchase

**Problem:**
```java
// Item fetched but not locked until much later
if (market.item3.get(j).index == p.data_yesno[1]) {
    it_receive = market.item3.get(j);
    break;
}
// ... lots of code...
if (!it_receive.is_processing.compareAndSet(false, true)) {
    Service.send_box_ThongBao_OK(p, "Vật phẩm đang được xử lý...");
}
```

**Scenario:**
1. Player A and B see same item for 1M extol
2. Both click buy at same time
3. Item's `is_processing` prevents dual purchase, but...
4. If item price changed between display and purchase, no re-validation

**Recommendation:**
```java
// Lock item BEFORE anything else
if (!it_receive.is_processing.compareAndSet(false, true)) {
    Service.send_box_ThongBao_OK(p, "Vật phẩm đang được xử lý...");
    return;
}
try {
    // Re-validate price in case seller changed it
    if (it_receive.price_market != expectedPrice) {
        Service.send_box_ThongBao_OK(p, "Giá vật phẩm đã thay đổi");
        return;
    }
    // ... proceed
} finally {
    it_receive.is_processing.set(false);
}
```

---

### 🟠 BUG #5: Trade - Negative Beri After Fee Deduction
**Severity:** HIGH  
**File:** [Trade.java](Trade.java#L250-L280)  
**Impact:** Player balance goes negative, corrupting economy records

**Problem:**
```java
// Lines 263-267
p.update_ngoc(-p.fee_trade);
target.update_ngoc(-target.fee_trade);
p.update_vang(-(p.money_trade * 130L) / 100L);  // Deduct beri with 30% fee
target.update_vang(-(target.money_trade * 130L) / 100L);
p.update_vang(target.money_trade);  // Add received beri
target.update_vang(p.money_trade);
// Nếu âm vàng → rollback
if (p.get_vang() < 0 || target.get_vang() < 0) {
    // ... cleanup code
}
```

**Problem Details:**
- Multiple deductions happen before validation
- If balance goes negative, items already transferred
- Rollback tries to recover but may not be atomic

**Example Scenario:**
- Player has 100K beri
- Tries to trade 80K beri + fee
- Fee: 80K * 1.3 = 104K total deduction
- Balance becomes -4K temporarily
- Rollback may fail if connection lost

**Recommendation:**
```java
// Validate total before ANY deduction
long totalCostP = (long)(p.fee_trade + (p.money_trade * 130L) / 100L);
long totalCostT = (long)(target.fee_trade + (target.money_trade * 130L) / 100L);

if (p.get_vang() < totalCostP || target.get_vang() < totalCostT) {
    Service.send_box_ThongBao_OK(p, "Không đủ beri/ruby để hoàn tất giao dịch");
    return;
}
// Only then proceed
```

---

### 🟠 BUG #6: Trade - Items Lost on Disconnect
**Severity:** HIGH  
**File:** [Trade.java](Trade.java#L280+)  
**Impact:** Items vanish from both players' inventories

**Problem:**
```java
// When both players lock trade:
// p.is_lock_trade = true
// items added to p.list_item_trade3 (NOT in bag)
// If connection drops before accept, items stuck in limbo
```

**Flow:**
1. Player A locks trade with items in `list_item_trade3`
2. Connection drops before confirming
3. `end_trade_by_disconnect` called but items not returned to bag
4. Items lost permanently

**Current Recovery Code (Insufficient):**
```java
// This tries to recover but incomplete
if (p.conn == null || !p.conn.connected) {
    end_trade_by_disconnect(p, target, 0, p.name);
}
```

**Recommendation:**
```java
public static void end_trade_by_disconnect(Player p, Player target, int type, String name) {
    // Return items to inventory BEFORE clearing
    for (Item_wear it : p.list_item_trade3) {
        if (p.item.able_bag() > 0) {
            p.item.add_item_bag3(it);
        }
    }
    for (ItemBag47 it47 : p.list_item_trade47) {
        p.item.add_item_bag47(it47.category, it47.id, it47.quant);
    }
    p.item.update_Inventory(-1, false);
    // Then cleanup
    p.list_item_trade3.clear();
    p.list_item_trade47.clear();
}
```

---

### 🟠 BUG #7: Market - Seller Payment Not Guaranteed
**Severity:** HIGH  
**File:** [ClientYesNo.java](ClientYesNo.java#L2720-L2730)  
**Impact:** Buyer gets item but seller never receives payment

**Problem:**
```java
// Buyer pays immediately:
if (!p.payVnd(it_receive.price_market)) {
    return;
}
// ... buyer receives item...
// But seller payment is NEVER processed!
```

**Issue:**
- Code doesn't add extol to seller's account
- Item marked as `type_market = 2` (sold) but payment lost
- Seller can only get money via "receive payment" dialog

**Current Code Gap:**
```java
it_receive.time_market = 0;
it_receive.type_market = 2;  // Marked as sold
// No: seller.update_vnd(+price);  <--- MISSING!
```

**Recommendation:**
```java
// After buyer payment confirmed
Player seller = Map.get_player_by_name_allmap(it_receive.seller);
if (seller != null) {
    seller.update_vnd(+(it_receive.price_market));
    seller.update_money();
    Service.send_box_ThongBao_OK(seller, 
        "Vật phẩm của bạn đã bán với giá " + Util.number_format(it_receive.price_market));
} else {
    // Seller offline - need to log to database for later collection
    SQL.gI().addSoldItemRevenue(it_receive.seller, it_receive.price_market);
}
```

---

## MEDIUM SEVERITY BUGS

### 🟡 BUG #8: Shop - Integer Overflow in Quantity Calculation
**Severity:** MEDIUM  
**File:** [Service.java](Service.java#L869)  
**Impact:** Wrong quantity purchased if values are large

**Problem:**
```java
if (TypeShop != 116 && TypeShop != 118 && (value == 1 || value == 20 || value == 500) && p.soluongmua > 0) {
    value = (short) p.soluongmua;  // <-- No bounds check!
}
```

**Issue:**
- `soluongmua` could be any integer
- Cast to `short` causes overflow: if `soluongmua = 40000`, becomes ~-25536
- Player pays for 1 item but receives negative quantity (error or nothing)

**Recommendation:**
```java
if (p.soluongmua > 0 && p.soluongmua <= DataTemplate.MAX_ITEM_IN_BAG) {
    value = (short) p.soluongmua;
} else {
    Service.send_box_ThongBao_OK(p, "Số lượng không hợp lệ!");
    return;
}
```

---

### 🟡 BUG #9: Market - Item Index Not Unique After Expiry
**Severity:** MEDIUM  
**File:** [Market.java](Market.java#L50-L100)  
**Impact:** Duplicate item IDs cause wrong item to be purchased

**Problem:**
```java
public static void update_at_market_index(Player p, int type) throws IOException {
    // Items marked as expired:
    if (template.item3.get(j).type_market == 1) {  // Expired
        template.item3.get(j).type_market = 3;  // Mark as expired
    }
    // But item.index is REUSED for new listings!
}
```

**Scenario:**
1. Item A listed with index #5
2. Item A expires (type_market = 3)
3. Item B listed, gets index #5 (recycled!)
4. Old buyer data still references index #5
5. Purchases item B instead of removed item A

**Recommendation:**
```java
// Use atomic counter for unique IDs
private static AtomicInteger nextItemIndex = new AtomicInteger(0);

public ItemMarket() {
    this.index = nextItemIndex.incrementAndGet();  // Always unique
}
```

---

### 🟡 BUG #10: Trade - Fee Bypass via Item Splitting
**Severity:** MEDIUM  
**File:** [Trade.java](Trade.java#L230-L240)  
**Impact:** Player reduces trade fees through clever item bundling

**Problem:**
```java
// Fee calculation
int fee_item4 = 0;
for (int i = 0; i < p.list_item_trade47.size(); i++) {
    fee_item4 += p.list_item_trade47.get(i).quant * 10;  // 10 per item
}
```

**Exploit:**
- 1 stack of 100 items = 1000 fee
- But 100 stacks of 1 item = still 1000 fee
- Player could artificially split items to reduce individual stack fees

**Recommendation:**
```java
// Calculate fee based on total quantity regardless of stacking
int totalQuantity = 0;
for (ItemBag47 item : p.list_item_trade47) {
    totalQuantity += item.quant;
}
fee_item4 = totalQuantity * 10;  // Fee based on total, not per-stack
```

---

## SUMMARY TABLE

| # | Bug | Severity | Component | Fix Difficulty |
|---|-----|----------|-----------|-----------------|
| 1 | Market Price No Validation | 🔴 CRITICAL | Market | Medium |
| 2 | Recharge Array Out of Bounds | 🔴 CRITICAL | ListTichNap | Easy |
| 3 | Shop Unlimited Stock | 🔴 CRITICAL | Service/Shop | Hard |
| 4 | Market Race Condition | 🟠 HIGH | Market/Trade | Hard |
| 5 | Trade Negative Beri | 🟠 HIGH | Trade | Medium |
| 6 | Trade Items Lost on DC | 🟠 HIGH | Trade | Medium |
| 7 | Market Seller No Payment | 🟠 HIGH | Market | Medium |
| 8 | Integer Overflow Qty | 🟡 MEDIUM | Shop | Easy |
| 9 | Item Index Reuse | 🟡 MEDIUM | Market | Medium |
| 10 | Trade Fee Bypass | 🟡 MEDIUM | Trade | Easy |

---

## RECOMMENDED PRIORITY ORDER

1. **First (Immediate):** Fix bugs #2, #8, #10 - Quick wins
2. **Second (This week):** Fix bugs #1, #5, #7 - Core functionality
3. **Third (This month):** Fix bugs #3, #4, #6, #9 - Architectural changes

---

## Testing Recommendations

1. **Unit Tests:**
   - Price validation (min/max bounds)
   - Array size checks before indexing
   - Trade fee calculations with edge cases

2. **Integration Tests:**
   - Simultaneous market purchases
   - Trade completion with network interruption
   - Recharge milestone unlock sequence

3. **Stress Tests:**
   - 100 concurrent market trades
   - Item price range validation (0 to Long.MAX_VALUE)
   - Large quantity shop purchases

---

*Report generated for project optimization and security audit.*
