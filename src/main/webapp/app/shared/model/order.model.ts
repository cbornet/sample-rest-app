import { ICustomer } from 'app/shared/model/customer.model';

export interface IOrder {
  id?: number;
  product?: string;
  cost?: number;
  customer?: ICustomer;
}

export class Order implements IOrder {
  constructor(public id?: number, public product?: string, public cost?: number, public customer?: ICustomer) {}
}
