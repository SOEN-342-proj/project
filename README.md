# SOEN 342 project

## Team Members

| Name | Student ID |
|------|------------|
| Malcolm Arcand Laliberté | 26334792 |
| Rafael Lenz | 40259696 |
| Jawad AlAqlani | 40272579 |

## Use-case diagram, including critical and non-critical use-cases
<img width="663" height="1043" alt="image" src="https://github.com/user-attachments/assets/ebf30a08-7239-4b76-9127-e6c9edf9ccd3" />

## UML domain model
<img width="1082" height="326" alt="image" src="https://github.com/user-attachments/assets/bf984c9d-ac21-4c31-b04f-1b8ff4a08f70" />

## System sequence diagrams to capture success and failure scenarios for critical usecases

<img width="419" height="329" alt="UC8_search_tasks" src="https://github.com/user-attachments/assets/ffa63a6b-3a46-428c-b4da-e973f80452a3" />
<img width="999" height="337" alt="UC7_view_tasks" src="https://github.com/user-attachments/assets/dc6285b5-2492-4e86-8cac-85d1e50bf7cc" />
<img width="542" height="660" alt="UC6_manage_tags" src="https://github.com/user-attachments/assets/22b5fecc-7a32-4e6a-bcc8-5f6cef1dd3e8" />
<img width="410" height="523" alt="UC5_manage_projects" src="https://github.com/user-attachments/assets/6c6d56cc-14e8-44dd-a281-87abbc3152bf" />
<img width="572" height="663" alt="UC4_manage_subtasks" src="https://github.com/user-attachments/assets/410bf96e-9ee2-4d01-ab49-1197c69a3b9b" />
<img width="846" height="353" alt="UC3_change_task_status" src="https://github.com/user-attachments/assets/e501a5a7-f5fa-4909-b677-6e583b15dcad" />
<img width="550" height="311" alt="UC2_update_task" src="https://github.com/user-attachments/assets/853f8c9c-c10e-4057-a726-feac2ca4902e" />
<img width="693" height="319" alt="UC1_create_task" src="https://github.com/user-attachments/assets/42d8eaf7-c95d-4f5b-849e-487e60ea42c7" />
<img width="420" height="396" alt="UC9_view_activity_history" src="https://github.com/user-attachments/assets/5e610dcf-43d9-469b-be2d-5f25812ebae0" />


## One fully-dressed scenario for a critical use-case, with at least one success and one failure case (equivalent system sequence diagram to be provided in the above)
1) Fully-dressed use case (critical): Create Task

Use Case Name: Create Task
Scope: Task Management System (black box)
Level: User goal
Primary Actor: User
Trigger: User wants to create a new task.

* Preconditions:

- System is available.

- User is able to create tasks.

* Success Guarantee (Postconditions):

- Task is created and stored (with required fields + defaults like status=open, creation date).

- Activity history records a “Task created” entry.

* Minimal Guarantee (Failure):

- No task is created; nothing is stored; user gets an error.

* Main Success Scenario

1. User requests to create a task.

2. System asks for task fields (title required; others optional).

3. User submits task details.

4. System validates the inputs.

5. System creates and stores the new task.

6. System records an activity entry (“Task created”).

7. System returns the new taskId to the user.

* Extensions (Failure case included)

4a. Missing title OR invalid priority

1. System rejects the request.

2. System returns error(message).

3. Use case ends (Minimal Guarantee).

(That structure follows the “fully dressed” idea: stakeholders/guarantees + main success + extensions.)

<img width="416" height="312" alt="image" src="https://github.com/user-attachments/assets/9299cbf9-adcf-473c-a48d-bccb8c3f242b" />
@startuml
actor User
participant ":System" as System

User -> System : createTask(title, description, priority, dueDate)

alt missing title OR priority invalid
  System --> User : error(message)
else valid input
  System --> User : taskId
end

@enduml



## Identification of system operations and operation contracts
