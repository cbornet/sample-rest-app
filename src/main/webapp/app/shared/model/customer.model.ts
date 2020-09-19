import { IOrder } from 'app/shared/model/order.model';

export interface ICustomer {
  id?: number;
  name?: string;
  orders?: IOrder[];
}

export class Customer implements ICustomer {
  constructor(public id?: number, public name?: string, public orders?: IOrder[]) {}
}
