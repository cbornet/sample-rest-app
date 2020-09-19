import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { HttpHeaders, HttpResponse } from '@angular/common/http';

import { TestModule } from '../../../test.module';
import { CustomerComponent } from 'app/entities/customer/customer.component';
import { CustomerService } from 'app/entities/customer/customer.service';
import { Customer } from 'app/shared/model/customer.model';

describe('Component Tests', () => {
  describe('Customer Management Component', () => {
    let comp: CustomerComponent;
    let fixture: ComponentFixture<CustomerComponent>;
    let service: CustomerService;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [TestModule],
        declarations: [CustomerComponent],
      })
        .overrideTemplate(CustomerComponent, '')
        .compileComponents();

      fixture = TestBed.createComponent(CustomerComponent);
      comp = fixture.componentInstance;
      service = fixture.debugElement.injector.get(CustomerService);
    });

    it('Should call load all on init', () => {
      // GIVEN
      const headers = new HttpHeaders().append('link', 'link;link');
      spyOn(service, 'query').and.returnValue(
        of(
          new HttpResponse({
            body: [new Customer(123)],
            headers,
          })
        )
      );

      // WHEN
      comp.ngOnInit();

      // THEN
      expect(service.query).toHaveBeenCalled();
      expect(comp.customers && comp.customers[0]).toEqual(jasmine.objectContaining({ id: 123 }));
    });
  });
});
