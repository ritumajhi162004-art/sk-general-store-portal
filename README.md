# S K General Store – Smart Inventory & Billing Web Portal

A lightweight, full-stack web application built with **Core Java** and **MySQL** to manage daily store inventory, product tracking, and customer billing with secure admin authentication.

## Key Features
* **Admin Authentication:** Cookie-based session management (`/login`, `/logout`) protecting all inventory and billing routes.
* **Inventory Management (CRUD):** Add new products, view live stock levels, and remove discontinued items.
* **Smart Billing with Live Search:** Real-time product search dropdown on the billing page that automatically updates stock quantities upon bill generation.
* **Clean Architecture:** Built using the DAO (Data Access Object) pattern (`ProductDAO`, `BillingDAO`) to separate database logic from HTTP routing.

## Tech Stack
* **Backend:** Core Java (`com.sun.net.httpserver`), JDBC
* **Database:** MySQL
* **Frontend:** HTML5, CSS3, Vanilla JavaScript

## How to Run Locally
1. Clone this repository:
   ```bash
   git clone [https://github.com/ritumajhi162004-art/sk-general-store-portal.git](https://github.com/ritumajhi162004-art/sk-general-store-portal.git)