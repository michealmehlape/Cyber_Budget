Cyber Budget Project Overview

- App Purpose
- This application is a personal finance management tool designed to help users track their income and expenses through a cloud-synchronized platform.
- It allows for detailed budgeting by category and provides real-time visual feedback on financial health and spending habits.
- The system uses cloud storage to ensure data is accessible across devices while maintaining high security standards.

- Core Features
- Expense and Income Tracking: Users can log every transaction with descriptions, amounts, dates, and categories.
- Cloud Synchronization: All data is stored in Firebase Firestore, allowing for real-time updates and data persistence.
- Dynamic Data Visualization: The dashboard includes a donut pie chart that displays spending distribution by category with percentage breakdowns.
- Category Management: Users can create custom categories with unique names and permanent color coding for easy identification.
- Budget Goal Setting: Each category can have a minimum and maximum spending goal to help maintain financial discipline.
- Automated Progress Tracking: Visual progress bars show how much of a category budget has been consumed, with color changes when approaching or exceeding limits.
- Daily Spending Intelligence: The Safe Today feature calculates a daily allowance based on the remaining budget and the days left in the current cycle.
- Cycle over Cycle Comparison: The app analyzes spending patterns between the current and previous budget cycles to show financial trends.
- Analytical Bar Charts: A detailed bar chart in the insights section compares spending across all active categories.
- Smart Financial Insights: A logic engine provides text-based advice and summaries based on spending behavior and budget status.

- Achievement Badges and How to Earn Them
- Active Logger Badge: This is awarded to users who consistently track their finances. To unlock it, you must log at least 10 separate expense entries in the system.
- Budget Pro Badge: This is an achievement for financial discipline. It is earned by completing a budget cycle without any of your spending categories exceeding their defined maximum goals.
- Finance Guru Badge: This is for advanced budgeting. To earn this, you must keep at least three different spending categories within their goal ranges simultaneously during a single cycle without any overspending.
- Saver Hero Badge: This is awarded for wealth building. It is unlocked when your total income for a specific budget cycle exceeds your total expenses for that same period.

- Security Implementation
- Biometric Authentication: The app supports fingerprint and face recognition login through the AndroidX Biometric library for secure access.
- Secure Session Management: A global session guard is implemented to enforce an idle-lock if the app is not used for 30 minutes.
- User Isolation: All data is strictly isolated using unique user identifiers and protected by server-side security rules to ensure privacy.

- Technical Specifications
- Built with Kotlin using modern Android development practices.
- Backend services provided by Firebase Authentication and Cloud Firestore.
- Data visualization powered by the MPAndroidChart library.
- User interface follows modern Material Design standards for a clean and professional look.

- Development Information
- Architected and developed by Cybersquad(Micheal Mehlape, Murendeni Nethezheni, Advice Ngobene).
- 
- 