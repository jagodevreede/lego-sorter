import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CalibrateComponent } from './calibrate.component';

describe('CalibrateComponent', () => {
  let component: CalibrateComponent;
  let fixture: ComponentFixture<CalibrateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CalibrateComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CalibrateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
