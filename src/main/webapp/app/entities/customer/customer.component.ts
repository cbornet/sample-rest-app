import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from './customer.service';
import { CustomerDeleteDialogComponent } from './customer-delete-dialog.component';

@Component({
  selector: 'jhi-customer',
  templateUrl: './customer.component.html',
})
export class CustomerComponent implements OnInit, OnDestroy {
  customers?: ICustomer[];
  eventSubscriber?: Subscription;

  constructor(protected customerService: CustomerService, protected eventManager: JhiEventManager, protected modalService: NgbModal) {}

  loadAll(): void {
    this.customerService.query().subscribe((res: HttpResponse<ICustomer[]>) => (this.customers = res.body || []));
  }

  ngOnInit(): void {
    this.loadAll();
    this.registerChangeInCustomers();
  }

  ngOnDestroy(): void {
    if (this.eventSubscriber) {
      this.eventManager.destroy(this.eventSubscriber);
    }
  }

  trackId(index: number, item: ICustomer): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  registerChangeInCustomers(): void {
    this.eventSubscriber = this.eventManager.subscribe('customerListModification', () => this.loadAll());
  }

  delete(customer: ICustomer): void {
    const modalRef = this.modalService.open(CustomerDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.customer = customer;
  }
}
