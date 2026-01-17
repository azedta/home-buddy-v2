# 🤖 HomeBuddy Backend — Robot-Assisted Medication Platform

A **production-style Spring Boot backend** powering *HomeBuddy*, a robot-assisted medication management system designed for assisted users, caregivers, and administrators.

This backend models **real-world medication schedules, robotic control, smart dispensers, and notification workflows**, with strong emphasis on **domain integrity, safety constraints, and event-driven logic**.

🔗 **Live Demo:** https://home-buddy-v2.vercel.app/                                 
🔗 **Frontend repository:** https://github.com/azedta/home-buddy-frontend-v2 

---

## 🚀 Overview

HomeBuddy solves a critical real-world problem:  
**ensuring medication adherence for assisted users through an intelligent robot + smart dispenser ecosystem**.

The backend is responsible for:
- Managing medication schedules and dose occurrences
- Enforcing safety and capacity constraints
- Coordinating with a robot telemetry & control layer
- Operating a smart pill dispenser
- Delivering reliable, deduplicated notifications
- Supporting caregivers and administrators via RBAC

This is **not a CRUD app** — it is a **stateful system** with time-based behavior, scheduled jobs, and cross-domain coordination.

---

## 🧠 High-Level Architecture

```
┌──────────────┐
│   Frontend   │
└──────┬───────┘
       │ REST API
       ▼
┌──────────────────────────┐
│      Spring Boot API     │
│──────────────────────────│
│  Auth & RBAC             │
│  Medication Engine       │
│  Dose Scheduling         │
│  Robot Control Layer     │
│  Smart Dispenser         │
│  Notification Engine     │
└─────────┬────────────────┘
          │
          ▼
┌──────────────────────────┐
│        PostgreSQL        │
└──────────────────────────┘
```

---

## 🧩 Core Domains

### 💊 Medication & Dosing Engine

Models **how medications are actually taken in real life**, not just stored.

**Key concepts:**
- `Dose`: a prescription rule (frequency, days, times, quantity)
- `DoseOccurrence`: a concrete scheduled instance in time
- `ScheduleEngine`: converts dose rules → scheduled timestamps

**Occurrence lifecycle:**
```
SCHEDULED → DUE → TAKEN
              ↓
           MISSED
```

**Key safeguards:**
- Prevents duplicate occurrences
- Enforces max daily capacity
- Locks old / already-taken doses
- Idempotent generation across windows

---

### ⏱️ Scheduling Engine

The scheduling engine:
- Respects start/end dates
- Supports custom days of week
- Uses explicit times or smart defaults
- Avoids unsafe times (e.g. middle of the night)
- Ensures deterministic ordering

```
Dose Rule
   ↓
ScheduleEngine
   ↓
LocalDateTime occurrences
   ↓
Persistence + Deduplication
```

---

### 🤖 Robot Telemetry & Control Layer

Represents a **realistic robot backend interface**, designed to be compatible with real hardware.

**Responsibilities:**
- Robot state management (battery, location, status)
- Command handling (move, deliver, return to dock)
- Telemetry heartbeat tracking
- Activity logging
- Health monitoring

**Robot lifecycle example:**
```
RESTING → MOVING → DELIVERING → RESTING
      ↘
   LOW BATTERY → RETURNING TO DOCK → CHARGING
```

This layer is **hardware-agnostic** and can later be connected to:
- MQTT
- WebSockets
- ROS
- Embedded firmware APIs

---

### 📦 Smart Dispenser System

Models a **physical pill dispenser** with per-day compartments.

**Key features:**
- 31 daily compartments
- Auto-loaded from medication schedule
- Idempotent refill logic
- Capacity-aware pill counts
- Real-time dispense on dose confirmation

**Dispense flow:**
```
Dose TAKEN
   ↓
Resolve Robot
   ↓
Select Day Compartment
   ↓
Decrement Pill Count
   ↓
Check Low / Empty State
```

---

### 🔔 Notification Engine (Advanced)

A **fully-featured notification system**, not just alerts.

**Capabilities:**
- Rule-based notifications
- Deduplication via stable keys
- Cooldown windows
- Severity levels
- User / caregiver / admin visibility
- Search, filters, unread counts

**Examples:**
- Medication due
- Missed dose
- Robot offline
- Dispenser empty
- Battery critical

Anti-spam logic ensures users are **informed, not overwhelmed**.

---

## 🔐 Security & Access Control

Implemented with **Spring Security + RBAC**.

### Roles
- `USER` — assisted user
- `CAREGEIVER` — manages assigned users
- `ADMIN` — full system access

### Enforcement
- Ownership checks (user vs caregiver)
- Admin-all mode for monitoring
- Role-based endpoint access

---

## ⏰ Scheduled Jobs

The system runs multiple background jobs to maintain correctness.

| Job | Frequency | Purpose |
|---|---|---|
| `OccurrenceScheduler` | Daily | Generate upcoming dose occurrences |
| `MedicationReminderJob` | Every 1 min | Due / confirm / missed reminders |
| `RobotHealthJob` | Every 10 sec | Detect offline robots |
| `DispenserHealthJob` | Every 5 min | Low / empty pill alerts |

All jobs are **idempotent and safe to rerun**.

---

## 🌐 API Overview (Selected)

### Medication
```
POST   /api/doses
GET    /api/doses
DELETE /api/doses/{id}
```

### Dose Occurrences
```
GET  /api/dose-occurrences
POST /api/dose-occurrences/generate
POST /api/dose-occurrences/{id}/taken
```

### Robot Control
```
GET  /api/robot/{id}/status
POST /api/robot/{id}/commands
GET  /api/robot/{id}/activities
```

### Dispenser
```
GET  /api/dispenser/{robotId}
POST /api/dispenser/{robotId}/reset-month
```

### Notifications
```
GET  /api/notifications
POST /api/notifications/{id}/read
GET  /api/notifications/unread-count
```

---

## 🛠 Tech Stack

- **Java**
- **Spring Boot**
- **Spring Security**
- **JPA / Hibernate**
- **PostgreSQL**
- **Scheduled Jobs**
- **Domain-Driven Design**
- **RESTful APIs**

---

## ⭐ What Makes This Project Stand Out

- Real-world domain modeling
- Strong consistency guarantees
- Event-driven architecture
- Time-based logic (not just requests)
- Anti-spam notification design
- Idempotent services
- Hardware-ready robot abstraction
- Production-style safety constraints

This backend is designed the way **real healthcare-adjacent systems are built**.

---

## 📌 Status

This backend is **feature-complete** and actively evolving alongside the frontend and robotic integration layers.

--- 

### 🌐 Deployment
The backend is deployed on **Render** using environment-based configuration and a production PostgreSQL database.

---

## 📄 License

This project is proprietary and protected under an All Rights Reserved license.

The source code is provided for viewing and evaluation purposes only as part of a personal portfolio.
Any use, reproduction, modification, or distribution without explicit permission from the author is prohibited.
