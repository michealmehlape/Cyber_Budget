# Smart Budget: Architectural Refactor & Technical Specification

Smart Budget is an Android-native financial management platform. This project represents a complete architectural transition from a localized SQLite (Room) persistence layer to a distributed, cloud-synchronized infrastructure powered by **Firebase**. The system is engineered for high-availability, hardware-level security, and efficient media processing.

---

## ✨ Core Features & POE Requirements

### 1. Advanced Data Visualization (Final POE)
*   **Dynamic Category Graph:** A high-performance `HorizontalBarChart` (via MPAndroidChart) visualizes spending across all user-defined categories.
*   **Goal Tracking:** The app now supports **Minimum and Maximum spending goals**. Progress bars are color-coded to provide instant visual feedback:
    *   🔵 **Blue**: Under-spending (Below Min Goal)
    *   🟢 **Green**: Healthy (Between Min and Max Goal)
    *   🔴 **Red**: Over-budget (Exceeded Max Goal)

### 2. Gamification & Rewards
To encourage financial discipline, the app includes a **Badges & Rewards system**:
*   🏆 **Active Logger**: Awarded for consistent expense tracking.
*   🏆 **Budget Pro**: Awarded for staying within your defined goal range.
*   🏆 **Finance Guru**: Awarded for mastering budget discipline across multiple categories.

---

## 🌟 Exclusive "Own Features"

### Feature A: The SAFE ZONE (Daily Spending Intelligence)
The dashboard now features a real-time **SAFE ZONE** calculation. 
*   **How it works:** It takes the user's remaining monthly budget and divides it by the days left in the month.
*   **Value:** It gives the user a specific "Daily Allowance" (e.g., *R150.00 / day*). This makes complex budgeting feel simple and manageable.

### Feature B: AI Spending Insights (Smart Analysis)
A rule-based logic engine that analyzes spending patterns over time.
*   **How it works:** The engine compares the current week's total spending against the previous week.
*   **Value:** It provides human-readable advice (e.g., *"Your spending is up 15% this week. Try to stick to your Safe Zone!"*), moving the app from a simple ledger to a smart financial assistant.

---

## 🏛 Technical Architecture

### 1. Cloud Ingestion & Data Tenancy
*   **Persistence:** Powered by **Cloud Firestore** for real-time synchronization.
*   **Security:** Every transaction is isolated by Firebase `UID` and protected by server-side rules.

### 2. Hardened Security Model
*   **Biometrics:** Integration with **AndroidX Biometric** for Fingerprint/Face ID.
*   **Session Guard:** 30-minute idle-lock enforced globally.

---

## 🛠 Engineering Stack
*   **Logic:** Kotlin (Coroutines & Structured Concurrency)
*   **Backend:** Firebase Auth & Firestore
*   **Charts:** MPAndroidChart
*   **UI:** Material Design 3 (M3)

---

**Developed and Architected by Micheal Mehlape**
