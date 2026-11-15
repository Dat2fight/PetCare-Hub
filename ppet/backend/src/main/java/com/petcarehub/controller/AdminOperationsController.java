package com.petcarehub.controller;
import com.petcarehub.service.*;
import com.petcarehub.dto.admin.OperationRequests;
import com.petcarehub.enums.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.Map;
@RestController @RequestMapping("/api/v1/admin") @RequiredArgsConstructor
public class AdminOperationsController {
    private final AdminOrderOperations orders;private final AdminInventoryOperations inventory;private final AdminAppointmentOperations appointments;private final AdminReviewOperations reviews;
    @GetMapping("/orders") @PreAuthorize("@perm.has('ORDER_MANAGE')")
    public Page<Map<String,Object>> orders(@RequestParam(required=false) OrderStatus status,@RequestParam(required=false) PaymentStatus payment,@RequestParam(required=false) String keyword,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(defaultValue="false") boolean refunds,@RequestParam(defaultValue="false") boolean expiring,Pageable pageable){return orders.list(status,payment,keyword,from,to,refunds,expiring,pageable);}
    @GetMapping("/orders/{id}") @PreAuthorize("@perm.has('ORDER_MANAGE')") public AdminOrderOperations.Detail order(@PathVariable Long id){return orders.detail(id);}
    @GetMapping("/orders/{id}/print") @PreAuthorize("@perm.has('ORDER_MANAGE')") public Object print(@PathVariable Long id){return orders.detail(id,true);}
    @PostMapping("/orders/{id}/transition") @PreAuthorize("@perm.has('ORDER_MANAGE')") public Object transition(@PathVariable Long id,@Valid @RequestBody OperationRequests.Action request){return orders.transition(id,request);}
    @PostMapping("/orders/{id}/request-refund") @PreAuthorize("@perm.has('ORDER_MANAGE')") public void requestRefund(@PathVariable Long id,@Valid @RequestBody OperationRequests.Note request){orders.requestRefund(id,request);}
    @PostMapping("/orders/{id}/mark-refunded") @PreAuthorize("@perm.has('ORDER_MANAGE')") public void refund(@PathVariable Long id,@Valid @RequestBody OperationRequests.Refund request){orders.refund(id,request);}
    @GetMapping("/orders/{id}/notes") @PreAuthorize("@perm.has('ORDER_MANAGE')") public Object notes(@PathVariable Long id,Pageable page){return orders.notes(id,page);}
    @PostMapping("/orders/{id}/notes") @PreAuthorize("@perm.has('ORDER_MANAGE')") public void addNote(@PathVariable Long id,@Valid @RequestBody OperationRequests.Note request){orders.addNote(id,request);}
    @GetMapping("/orders/{id}/payments") @PreAuthorize("@perm.has('ORDER_MANAGE')") public Object payments(@PathVariable Long id,Pageable page){return orders.payments(id,page);}
    @GetMapping("/inventory") @PreAuthorize("@perm.has('INVENTORY_MANAGE')") public Object inventory(@RequestParam(required=false) String keyword,@RequestParam(defaultValue="false") boolean low,Pageable page){return inventory.list(keyword,low,page);}
    @PostMapping("/inventory/movements") @PreAuthorize("@perm.has('INVENTORY_MANAGE')") public void move(@Valid @RequestBody OperationRequests.Stock request){inventory.move(request);}
    @GetMapping("/inventory/transactions") @PreAuthorize("@perm.has('INVENTORY_MANAGE')") public Object history(@RequestParam(required=false) Long productId,@RequestParam(required=false) TransactionType type,@RequestParam(required=false) Long actor,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,Pageable page){return inventory.history(productId,type,actor,from,to,page);}
    @GetMapping("/appointments") @PreAuthorize("@perm.has('APPOINTMENT_MANAGE') or @perm.has('ASSIGNED_APPOINTMENT_MANAGE')") public Object appointments(@RequestParam(required=false) Long staff,@RequestParam(required=false) Long serviceId,@RequestParam(required=false) AppointmentStatus status,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(defaultValue="false") boolean unassigned,@RequestParam(defaultValue="false") boolean actionable,Pageable page){return appointments.list(staff,serviceId,status,from,to,unassigned,actionable,page);}
    @GetMapping("/appointments/{id}") @PreAuthorize("@perm.has('APPOINTMENT_MANAGE') or @perm.has('ASSIGNED_APPOINTMENT_MANAGE')") public Object appointment(@PathVariable Long id){return appointments.detail(id);}
    @GetMapping("/appointments/{id}/eligible-staff") @PreAuthorize("@perm.has('APPOINTMENT_MANAGE')") public Object eligible(@PathVariable Long id,Pageable page){return appointments.eligible(id,page);}
    @PostMapping("/appointments/{id}/assign") @PreAuthorize("@perm.has('APPOINTMENT_MANAGE')") public void assign(@PathVariable Long id,@Valid @RequestBody OperationRequests.Assign request){appointments.assign(id,request);}
    @PostMapping("/appointments/{id}/status") @PreAuthorize("@perm.has('APPOINTMENT_MANAGE') or @perm.has('ASSIGNED_APPOINTMENT_MANAGE')") public void status(@PathVariable Long id,@Valid @RequestBody OperationRequests.Status request){appointments.status(id,request);}
    @GetMapping("/reviews") @PreAuthorize("@perm.has('REVIEW_MANAGE')") public Object reviews(@RequestParam(defaultValue="PENDING") String status,@RequestParam(required=false) String type,@RequestParam(required=false) Long target,Pageable page){return reviews.list(status,type,target,page);}
    @GetMapping("/reviews/count") @PreAuthorize("@perm.has('REVIEW_MANAGE')") public Object pending(){return reviews.count();}
    @PostMapping("/reviews/{id}/moderate") @PreAuthorize("@perm.has('REVIEW_MANAGE')") public void moderate(@PathVariable Long id,@Valid @RequestBody OperationRequests.Moderate request){reviews.moderate(id,request);}
}
