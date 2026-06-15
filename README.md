- Smart Budget Overview
- This application is a personal finance management tool used to monitor income and expenses.
- It stores data in a cloud database to keep financial records synced across different devices.
- The app helps users maintain a budget by providing visual feedback on spending categories and goals.

------------------------Features We Implemented---------------------------------------------------
- Income and expense tracking with details for amount, date, description, and category.
- visuial graphs showing amount spent per category in a specific cycle 
- Cloud synchronization using Firebase Firestore for real-time data persistence.
- Interactive donut pie chart on the dashboard displaying spending by category.
- Percentage values displayed inside the chart slices and category names outside with connecting lines.
- Custom category management where users can set names and permanent colors.
- Budget goal setting for each category, including minimum and maximum spending limits.
- Visual progress bars that track budget minimum and maximum goals and change color based on spending status.
- Transaction history list for reviewing and managing past entries.
- Image attachment support for saving photos of receipts with transactions.
- Biometric security integration for fingerprint and facial recognition access.
- Automatic session locking after 30 minutes of user inactivity.
- Customizable budget cycles allowing users to set specific start dates for tracking.
- Navigation system for switching between home, insights, activity, and profile sections.
- gamifications element awarding the user badges for meeting certain budget goals

-----------------------------OWN FEATURES------------------------------------
- Safe Zone Implementation
- AI spending Analysis

-----------------------------HOW IT WORKS------------------------------------

- Safe Zone Feature
- The Safe Zone, also displayed as Safe Today, is a daily spending intelligence tool.
- It calculates the total remaining budget by taking all category maximum goals and subtracting current expenses.
- The remaining balance is divided by the number of days left in the current budget cycle.
- It provides a specific daily allowance figure to show how much can be spent each day without exceeding the budget.
- This feature breaks down long-term monthly goals into a single, manageable daily target.

- AI Spending Analysis
- The AI analysis uses a rule-based logic engine to evaluate financial behavior.
- It compares the total spending of the current cycle with the total from the previous cycle.
- The engine identifies which categories are over-budget or approaching their limits and provides specific alerts.
- It generates human-readable summaries that explain spending trends in percentages.
- The tool monitors logging frequency to ensure the user is maintaining consistent records.
- It provides advice on how to adjust spending to align with the Safe Zone and overall financial goals.

- How to Get Badges
- Active Logger: This is earned by logging at least 10 separate expense entries to demonstrate consistent tracking.
- Budget Pro: This is awarded for completing a full budget cycle without any category exceeding its maximum spending limit.
- Finance Guru: This is earned by keeping at least three different categories within their goal ranges simultaneously during a cycle.
- Saver Hero: This is unlocked if the total income for a budget cycle is higher than the total expenses for that same period.

- Security and Data Protection
- User isolation is maintained by associating all data with unique identifiers in the database.
- Biometric authentication is handled through the AndroidX Biometric library for secure logins.
- Idle-lock security is enforced globally to protect sensitive financial data.
- Server-side security rules prevent unauthorized access or modification of user records.

- Technical Specifications
- Built using the Kotlin and standard Android development tools.
- Uses Firebase Authentication and Cloud Firestore for backend services.
- Data visualization and charts are implemented using the MPAndroidChart library.
- The interface is built with Material Design to ensure a consistent appearance.

- Development Team
- Developed by Micheal Mehlape, Murendeni Nethezheni, and Advice Ngobene.
